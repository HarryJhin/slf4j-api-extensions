package io.github.harryjhin.slf4j.extensions.compiler.backend

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Slf4jIrTransformer(
    private val context: IrPluginContext,
    private val propertyName: String,
) : IrVisitorVoid() {

    private val loggerClassId = ClassId(FqName("org.slf4j"), Name.identifier("Logger"))

    private val getLoggerSymbol: IrSimpleFunctionSymbol by lazy {
        val candidates = context.referenceFunctions(
            CallableId(ClassId(FqName("org.slf4j"), Name.identifier("LoggerFactory")), Name.identifier("getLogger"))
        )
        candidates.firstOrNull { symbol ->
            symbol.signature?.toString()?.contains("String") == true
        } ?: candidates.first()
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != Slf4jExtensionsPluginKey) return
        if (declaration.name.asString() != propertyName) return

        val backingField = declaration.backingField ?: return
        val parentClass = declaration.parent as? IrClass ?: return

        val builder = DeclarationIrBuilder(context, backingField.symbol)
        val className = buildClassName(parentClass)

        backingField.initializer = builder.irExprBody(
            builder.irCall(getLoggerSymbol).also { call ->
                val paramIndex = call.symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.Regular }
                call.arguments[paramIndex] = builder.irString(className)
            }
        )
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val origin = declaration.origin
        if (origin !is IrDeclarationOrigin.GeneratedByPlugin || origin.pluginKey != Slf4jExtensionsPluginKey) return

        val levelName = declaration.name.asString()
        if (levelName !in LOG_LEVELS) return

        val regularParams = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
        val hasThrowable = regularParams.size == 2
        val parentClass = declaration.parent as? IrClass ?: return

        val logGetter = findLogGetter(parentClass) ?: return

        val isEnabledName = "is${levelName.replaceFirstChar { it.uppercase() }}Enabled"
        val isEnabledSymbol = context.referenceFunctions(
            CallableId(loggerClassId, Name.identifier(isEnabledName))
        ).firstOrNull { symbol ->
            symbol.signature?.toString()?.contains("Marker") != true
        } ?: return

        val logMethodSymbol = context.referenceFunctions(
            CallableId(loggerClassId, Name.identifier(levelName))
        ).firstOrNull { symbol ->
            val sig = symbol.signature?.toString() ?: ""
            if (hasThrowable) {
                sig.contains("Throwable") && !sig.contains("Marker")
            } else {
                !sig.contains("Throwable") && !sig.contains("Marker") && !sig.contains("Object")
            }
        } ?: return

        val invokeSymbol = context.referenceFunctions(
            CallableId(ClassId(FqName("kotlin"), Name.identifier("Function0")), Name.identifier("invoke"))
        ).firstOrNull() ?: return

        val builder = DeclarationIrBuilder(context, declaration.symbol)
        val dispatchParam = declaration.dispatchReceiverParameter ?: return
        val messageParam = regularParams.last()

        declaration.body = builder.irBlockBody {
            val logVal = irTemporary(
                irCall(logGetter).apply {
                    dispatchReceiver = irGet(dispatchParam)
                }
            )

            val msgExpr = irCall(invokeSymbol).apply {
                dispatchReceiver = irGet(messageParam)
            }

            val logCall = irCall(logMethodSymbol).also { call ->
                call.dispatchReceiver = irGet(logVal)
                val logParams = call.symbol.owner.parameters.filter { it.kind == IrParameterKind.Regular }
                call.arguments[logParams[0].indexInParameters] = msgExpr
                if (hasThrowable) {
                    call.arguments[logParams[1].indexInParameters] = irGet(regularParams[0])
                }
            }

            +irIfThen(
                context.irBuiltIns.unitType,
                irCall(isEnabledSymbol).apply { dispatchReceiver = irGet(logVal) },
                logCall,
            )
        }
    }

    private fun findLogGetter(irClass: IrClass): IrSimpleFunction? {
        for (decl in irClass.declarations) {
            if (decl is IrProperty && decl.name.asString() == propertyName) {
                return decl.getter
            }
        }
        return null
    }

    private fun buildClassName(irClass: IrClass): String {
        val parts = mutableListOf(irClass.name.asString())
        var parent = irClass.parent
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

    companion object {
        private val LOG_LEVELS = setOf("trace", "debug", "info", "warn", "error")
    }
}
