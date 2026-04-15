package io.github.harryjhin.slf4j.extensions.compiler.backend

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Slf4jIrTransformer(
    private val pluginContext: IrPluginContext,
    private val propertyName: String,
) : IrElementTransformerVoid() {

    private val pluginOrigin = IrDeclarationOrigin.GeneratedByPlugin(Slf4jExtensionsPluginKey)

    private val loggerClassSymbol: IrClassSymbol by lazy {
        pluginContext.referenceClass(ClassId(FqName("org.slf4j"), Name.identifier("Logger")))
            ?: error("Cannot find org.slf4j.Logger on classpath")
    }

    private val loggerFactoryGetLogger: IrSimpleFunctionSymbol by lazy {
        val factoryClassId = ClassId(FqName("org.slf4j"), Name.identifier("LoggerFactory"))
        val factoryClass = pluginContext.referenceClass(factoryClassId)
            ?: error("Cannot find org.slf4j.LoggerFactory on classpath")
        // Use getLogger(String) overload — avoids complex class reference IR generation
        factoryClass.functions.first { func ->
            func.owner.name.asString() == "getLogger" &&
            func.owner.valueParameters.size == 1 &&
            (func.owner.valueParameters[0].type as? IrSimpleType)?.classifier == pluginContext.irBuiltIns.stringClass
        }
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.declarations.toList().forEach { member ->
            when (member) {
                is IrProperty -> if (member.origin == pluginOrigin && member.name.asString() == propertyName) {
                    fillPropertyInitializer(member, declaration)
                }
                is IrSimpleFunction -> if (member.origin == pluginOrigin && member.name.asString() in LOG_LEVELS) {
                    fillFunctionBody(member, declaration)
                }
                else -> {}
            }
        }
        return super.visitClass(declaration)
    }

    private fun fillPropertyInitializer(property: IrProperty, parentClass: IrClass) {
        val backingField = property.backingField ?: return
        val builder = DeclarationIrBuilder(pluginContext, backingField.symbol)

        // LoggerFactory.getLogger("com.example.MyClass")
        val className = parentClass.fqNameWhenAvailable?.asString() ?: parentClass.name.asString()
        val getLoggerCall = builder.irCall(loggerFactoryGetLogger).apply {
            putValueArgument(0, builder.irString(className))
        }

        backingField.initializer = pluginContext.irFactory.createExpressionBody(
            builder.startOffset, builder.endOffset, getLoggerCall
        )
    }

    private fun fillFunctionBody(function: IrSimpleFunction, parentClass: IrClass) {
        val levelName = function.name.asString()
        val hasThrowable = function.valueParameters.size == 2
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)

        val logProperty = parentClass.properties.firstOrNull { it.name.asString() == propertyName } ?: return
        val logGetter = logProperty.getter ?: return

        val isEnabledName = "is${levelName.replaceFirstChar { it.uppercase() }}Enabled"
        val isEnabledMethod = loggerClassSymbol.functions.firstOrNull {
            it.owner.name.asString() == isEnabledName && it.owner.valueParameters.isEmpty()
        } ?: return

        val logMethod: IrSimpleFunctionSymbol = findLogMethod(levelName, hasThrowable) ?: return

        // Resolve Function0.invoke() before entering builder
        val messageParam = function.valueParameters.last()
        val messageType = messageParam.type as? IrSimpleType ?: return
        val function0Class = messageType.classifier.owner as? IrClass ?: return
        val invokeMethod: IrSimpleFunctionSymbol = function0Class.functions.firstOrNull {
            it.name.asString() == "invoke" && it.valueParameters.isEmpty()
        }?.symbol ?: return

        function.body = builder.irBlockBody {
            // val $log = this.log
            val logVal = irTemporary(
                irCall(logGetter).apply {
                    dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                }
            )

            // if ($log.isXxxEnabled) $log.xxx(message.invoke()) or $log.xxx(message.invoke(), throwable)
            val invokeMessage = irCall(invokeMethod).apply {
                dispatchReceiver = irGet(messageParam)
            }

            val logCall = irCall(logMethod).apply {
                dispatchReceiver = irGet(logVal)
                putValueArgument(0, invokeMessage)
                if (hasThrowable) {
                    putValueArgument(1, irGet(function.valueParameters[0]))
                }
            }

            +irIfThen(
                pluginContext.irBuiltIns.unitType,
                irCall(isEnabledMethod).apply {
                    dispatchReceiver = irGet(logVal)
                },
                logCall,
            )
        }
    }

    private fun findLogMethod(levelName: String, hasThrowable: Boolean): IrSimpleFunctionSymbol? {
        val stringClassSymbol = pluginContext.irBuiltIns.stringClass
        val throwableClassId = ClassId(FqName("java.lang"), Name.identifier("Throwable"))
        val throwableClassSymbol = pluginContext.referenceClass(throwableClassId)

        return loggerClassSymbol.functions.firstOrNull { func ->
            val f = func.owner
            if (f.name.asString() != levelName) return@firstOrNull false
            if (hasThrowable) {
                f.valueParameters.size == 2 &&
                (f.valueParameters[0].type as? IrSimpleType)?.classifier == stringClassSymbol &&
                (f.valueParameters[1].type as? IrSimpleType)?.classifier == throwableClassSymbol
            } else {
                f.valueParameters.size == 1 &&
                (f.valueParameters[0].type as? IrSimpleType)?.classifier == stringClassSymbol
            }
        }
    }

    companion object {
        private val LOG_LEVELS = setOf("trace", "debug", "info", "warn", "error")
    }
}
