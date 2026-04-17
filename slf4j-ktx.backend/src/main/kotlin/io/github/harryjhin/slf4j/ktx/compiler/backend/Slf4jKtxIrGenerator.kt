package io.github.harryjhin.slf4j.ktx.compiler.backend

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Fills bodies for the plugin-generated Companion/object members:
 *  - `log` property: ensures a backing field, installs a `LoggerFactory.getLogger(FQN)`
 *    initializer, and a `irGetField` getter body.
 *  - level functions (`trace/debug/info/warn/error` × 2 overloads): installs
 *    `if (log.isXxxEnabled) log.xxx(message.invoke() [, throwable])`.
 *
 * Works uniformly for Companion and standalone `@Slf4j object` — the only semantic difference
 * is the logger name resolution in [loggerNameFor].
 */
@Suppress("DEPRECATION")
internal class Slf4jKtxIrGenerator(
    private val ctx: Slf4jKtxPluginContext,
) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> Unit
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        if (!declaration.isFromPlugin(ctx.afterK2)) return
        if (declaration.name != Slf4jKtxEntityNames.LOG_PROPERTY_ID) return
        val site = declaration.parent as? IrClass ?: return

        ensureBackingField(declaration, site)
        fillInitializer(declaration, site)
        fillGetterBody(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (!declaration.isFromPlugin(ctx.afterK2)) return
        val levelName = declaration.name.asString()
        if (levelName !in LOG_LEVEL_STRINGS) return
        if (declaration.body != null) return

        val site = declaration.parent as? IrClass ?: return
        val logGetter = findLogGetter(site) ?: return
        val dispatchParam = declaration.dispatchReceiverParameter ?: return

        val regularParams = declaration.valueParameters
        val hasThrowable = regularParams.size == 2
        val messageParam = regularParams.last()

        val isEnabledSymbol = resolveIsEnabled(levelName)
        val logMethodSymbol = resolveLogMethod(levelName, hasThrowable)

        val builder = DeclarationIrBuilder(ctx, declaration.symbol)
        declaration.body = builder.irBlockBody {
            val logVal = irTemporary(
                irCall(logGetter).apply { this.dispatchReceiver = irGet(dispatchParam) }
            )
            val msgExpr = irCall(ctx.function0InvokeSymbol).apply {
                dispatchReceiver = irGet(messageParam)
            }
            val logCall = irCall(logMethodSymbol).also { call ->
                call.dispatchReceiver = irGet(logVal)
                call.putValueArgument(0, msgExpr)
                if (hasThrowable) {
                    call.putValueArgument(1, irGet(regularParams[0]))
                }
            }
            +irIfThen(
                ctx.irBuiltIns.unitType,
                irCall(isEnabledSymbol).apply { dispatchReceiver = irGet(logVal) },
                logCall,
            )
        }
    }

    // ---------------- property helpers ----------------

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
        val builder = DeclarationIrBuilder(ctx, backingField.symbol)
        val className = loggerNameFor(site)
        backingField.initializer = builder.irExprBody(
            builder.irCall(ctx.getLoggerSymbol).apply {
                putValueArgument(0, builder.irString(className))
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

    private fun findLogGetter(site: IrClass): IrSimpleFunction? {
        for (decl in site.declarations) {
            if (decl is IrProperty && decl.name == Slf4jKtxEntityNames.LOG_PROPERTY_ID) {
                return decl.getter
            }
        }
        return null
    }

    // ---------------- level-function helpers ----------------

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

    /**
     * Companion: logger name = enclosing class's FQN (e.g. `com.example.OrderService`, not
     * `com.example.OrderService.Companion`). Standalone `@Slf4j object`: the object's own FQN.
     */
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
