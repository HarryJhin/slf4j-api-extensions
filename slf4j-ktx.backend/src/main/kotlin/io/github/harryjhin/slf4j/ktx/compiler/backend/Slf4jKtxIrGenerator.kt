package io.github.harryjhin.slf4j.ktx.compiler.backend

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Backend IR transformer with two responsibilities:
 *
 *  1. **Fill `log: Logger` property bodies** on plugin-synthesized Companion/object sites.
 *     The K1/K2 frontends declare the property; we install the backing field, the
 *     `LoggerFactory.getLogger(FQN)` initializer, and the trivial getter body.
 *
 *  2. **Rewrite call sites** to `io.github.harryjhin.slf4j.ktx.<level>()` runtime extensions
 *     when the extension receiver's class carries a plugin-synthesized `log`. The rewrite
 *     produces the equivalent of:
 *     ```kotlin
 *     val tmp = Foo.Companion.log
 *     if (tmp.isXxxEnabled) tmp.xxx(message.invoke() [, throwable])
 *     ```
 *     No [org.slf4j.LoggerFactory.getLogger] call, no reflection — a direct static-field
 *     read. Classes without plugin-synthesized `log` fall through to the runtime extension's
 *     default body, which performs a `LoggerFactory.getLogger(T::class.java)` cache lookup
 *     (the fallback path).
 *
 * IDE resolves the extension calls against `slf4j-ktx-core` regardless; the IR rewrite is
 * purely a backend optimization, invisible to the frontend.
 */
@Suppress("DEPRECATION")
internal class Slf4jKtxIrGenerator(
    private val ctx: Slf4jKtxPluginContext,
) : IrElementTransformerVoidWithContext() {

    override fun visitClassNew(declaration: IrClass): IrStatement {
        for (decl in declaration.declarations) {
            if (decl is IrProperty
                && decl.isFromPlugin(ctx.afterK2)
                && decl.name == Slf4jKtxEntityNames.LOG_PROPERTY_ID
            ) {
                val site = decl.parent as? IrClass ?: continue
                ensureBackingField(decl, site)
                fillInitializer(decl, site)
                fillGetterBody(decl)
            }
        }
        return super.visitClassNew(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val rewritten = maybeRewriteLevelCall(expression)
        return rewritten ?: super.visitCall(expression)
    }

    /**
     * Returns the rewritten expression when [expression] targets one of our runtime level
     * extensions AND the receiver's class hosts a plugin-generated `log`. Otherwise null —
     * the caller falls through to the runtime extension's default body.
     */
    private fun maybeRewriteLevelCall(expression: IrCall): IrExpression? {
        val callee = expression.symbol.owner
        val fqName = callee.kotlinFqName
        if (fqName.parent() != Slf4jKtxEntityNames.LOGGER_EXTENSIONS_PACKAGE) return null
        val levelName = fqName.shortName().asString()
        if (levelName !in LOG_LEVEL_STRINGS) return null

        val extReceiver = expression.extensionReceiver ?: return null
        val receiverClass = extReceiver.type.classOrNull?.owner ?: return null

        // @Slf4j object uses the object itself as the log host; classes use their Companion.
        val logSite: IrClass = when {
            receiverClass.kind == ClassKind.OBJECT && !receiverClass.isCompanion -> receiverClass
            else -> receiverClass.companionObject() ?: return null
        }

        val logProperty = logSite.declarations.filterIsInstance<IrProperty>()
            .firstOrNull { it.name == Slf4jKtxEntityNames.LOG_PROPERTY_ID } ?: return null
        val logField = logProperty.backingField ?: return null

        val argCount = expression.valueArgumentsCount
        val hasThrowable = argCount == 2
        val messageLambda = expression.getValueArgument(if (hasThrowable) 1 else 0) ?: return null
        val throwableArg = if (hasThrowable) expression.getValueArgument(0) else null

        val scopeSymbol = currentScope?.scope?.scopeOwnerSymbol ?: return null
        val builder = DeclarationIrBuilder(ctx, scopeSymbol, expression.startOffset, expression.endOffset)
        val isEnabledSymbol = resolveIsEnabled(levelName)
        val logMethodSymbol = resolveLogMethod(levelName, hasThrowable)

        return builder.irBlock(resultType = ctx.irBuiltIns.unitType) {
            val logTmp = irTemporary(irGetField(irGetObject(logSite.symbol), logField))
            val msgCall = irCall(ctx.function0InvokeSymbol).apply {
                dispatchReceiver = messageLambda
            }
            val logCall = irCall(logMethodSymbol).apply {
                dispatchReceiver = irGet(logTmp)
                putValueArgument(0, msgCall)
                if (hasThrowable) putValueArgument(1, throwableArg)
            }
            +irIfThen(
                ctx.irBuiltIns.unitType,
                irCall(isEnabledSymbol).apply { dispatchReceiver = irGet(logTmp) },
                logCall,
            )
        }
    }

    // ---------------- log property body filling ----------------

    private fun ensureBackingField(prop: IrProperty, site: IrClass) {
        if (prop.backingField != null) return
        val loggerType = prop.getter?.returnType ?: return
        prop.backingField = ctx.irFactory.createField(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
            name = prop.name,
            visibility = DescriptorVisibilities.PRIVATE,
            symbol = IrFieldSymbolImpl(),
            type = loggerType,
            isFinal = true,
            isStatic = false,
            isExternal = false,
        ).also {
            it.parent = site
            it.correspondingPropertySymbol = prop.symbol
        }
    }

    private fun fillInitializer(prop: IrProperty, site: IrClass) {
        val backingField = prop.backingField ?: return
        if (backingField.initializer != null) return
        val builder = DeclarationIrBuilder(ctx, backingField.symbol)
        val loggerName = loggerNameFor(site)
        backingField.initializer = builder.irExprBody(
            builder.irCall(ctx.getLoggerSymbol).apply {
                putValueArgument(0, builder.irString(loggerName))
            }
        )
    }

    private fun fillGetterBody(prop: IrProperty) {
        val getter = prop.getter ?: return
        if (getter.body != null) return
        val dispatch = getter.dispatchReceiverParameter ?: return
        val backingField = prop.backingField ?: return
        val builder = DeclarationIrBuilder(ctx, getter.symbol)
        getter.body = builder.irExprBody(
            builder.irGetField(builder.irGet(dispatch), backingField)
        )
    }

    // ---------------- Logger API symbol resolution ----------------

    private fun resolveIsEnabled(levelName: String): IrSimpleFunctionSymbol {
        val isEnabledName = "is${levelName.replaceFirstChar { it.uppercase() }}Enabled"
        return ctx.loggerClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == isEnabledName && it.valueParameters.isEmpty() }
            .symbol
    }

    private fun resolveLogMethod(levelName: String, hasThrowable: Boolean): IrSimpleFunctionSymbol =
        ctx.loggerClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == levelName }
            .first { fn ->
                if (hasThrowable) {
                    fn.valueParameters.size == 2 &&
                        fn.valueParameters[1].type.classOrNull?.owner?.name?.asString() == "Throwable"
                } else {
                    fn.valueParameters.size == 1
                }
            }
            .symbol

    // ---------------- utilities ----------------

    /** Companion: enclosing class FQN. Standalone `@Slf4j object`: the object's own FQN. */
    private fun loggerNameFor(site: IrClass): String {
        val target = if (site.isCompanion) {
            (site.parent as? IrClass) ?: return buildFqn(site)
        } else {
            site
        }
        return buildFqn(target)
    }

    private fun buildFqn(cls: IrClass): String {
        val parts = mutableListOf(cls.name.asString())
        var parent = cls.parent
        while (parent is IrClass) {
            parts.add(0, parent.name.asString())
            parent = parent.parent
        }
        if (parent is IrPackageFragment) {
            val pkg = parent.packageFqName.asString()
            if (pkg.isNotEmpty()) return "$pkg.${parts.joinToString(".")}"
        }
        return parts.joinToString(".")
    }

    private companion object {
        val LOG_LEVEL_STRINGS: Set<String> =
            Slf4jKtxEntityNames.LOG_LEVEL_NAMES.map { it.asString() }.toSet()
    }
}
