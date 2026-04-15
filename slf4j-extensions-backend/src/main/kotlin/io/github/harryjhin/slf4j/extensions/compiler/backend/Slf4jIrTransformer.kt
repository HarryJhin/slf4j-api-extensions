package io.github.harryjhin.slf4j.extensions.compiler.backend

import io.github.harryjhin.slf4j.extensions.compiler.Slf4jExtensionsPluginKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
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
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Suppress("DEPRECATION")
class Slf4jIrTransformer(
    private val context: IrPluginContext,
    private val propertyName: String,
) : IrVisitorVoid() {

    private val loggerClassId = ClassId(FqName("org.slf4j"), Name.identifier("Logger"))

    private val getLoggerSymbol: IrSimpleFunctionSymbol by lazy {
        val factoryClassId = ClassId(FqName("org.slf4j"), Name.identifier("LoggerFactory"))
        val factoryClass = context.referenceClass(factoryClassId)
            ?: error("Cannot find org.slf4j.LoggerFactory")
        factoryClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "getLogger" }
            .first { func ->
                val params = func.parameters.filter { it.kind == IrParameterKind.Regular }
                params.size == 1 &&
                    params[0].type.classOrNull?.owner?.name?.asString() == "String"
            }
            .symbol
    }

    private val invokeSymbol: IrSimpleFunctionSymbol by lazy {
        context.referenceFunctions(
            CallableId(
                ClassId(FqName("kotlin"), Name.identifier("Function0")),
                Name.identifier("invoke"),
            )
        ).first()
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment ->
                element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun isPluginGenerated(declaration: IrDeclaration): Boolean =
        if (context.afterK2) {
            // K2: FIR-generated declarations tagged with GeneratedByPlugin origin
            declaration.origin == IrDeclarationOrigin.GeneratedByPlugin(Slf4jExtensionsPluginKey)
        } else {
            // K1: synthetic descriptors lowered with DEFINED origin, check descriptor kind
            (declaration.descriptor as? CallableMemberDescriptor)?.kind ==
                CallableMemberDescriptor.Kind.SYNTHESIZED
        }

    override fun visitProperty(declaration: IrProperty) {
        if (!isPluginGenerated(declaration)) return
        if (declaration.name.asString() != propertyName) return
        // Note: K2 has a stub initializer from withGeneratedDefaultInitializer(),
        // K1 has no initializer. Both need to be (re)filled.

        val parentClass = declaration.parent as? IrClass ?: return

        // K1: psi2ir doesn't create backing field for synthetic properties
        if (declaration.backingField == null) {
            val loggerType = declaration.getter?.returnType ?: return
            declaration.backingField = context.irFactory.createField(
                startOffset = -1,
                endOffset = -1,
                origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD,
                name = declaration.name,
                visibility = org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PRIVATE,
                symbol = org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl(),
                type = loggerType,
                isFinal = true,
                isStatic = false,
                isExternal = false,
            ).also {
                it.parent = parentClass
                it.correspondingPropertySymbol = declaration.symbol
            }
        }
        val backingField = declaration.backingField ?: return

        val builder = DeclarationIrBuilder(context, backingField.symbol)
        val className = buildClassName(parentClass)

        backingField.initializer = builder.irExprBody(
            builder.irCall(getLoggerSymbol).also { call ->
                val paramIndex = call.symbol.owner.parameters.indexOfFirst {
                    it.kind == IrParameterKind.Regular
                }
                call.arguments[paramIndex] = builder.irString(className)
            }
        )

        // K1: getter body must be generated explicitly (K2 FIR→IR does this automatically)
        val getter = declaration.getter
        if (getter != null && getter.body == null) {
            val getterBuilder = DeclarationIrBuilder(context, getter.symbol)
            val getterDispatch = getter.dispatchReceiverParameter
            if (getterDispatch != null) {
                getter.body = getterBuilder.irExprBody(
                    getterBuilder.irGetField(
                        getterBuilder.irGet(getterDispatch),
                        backingField,
                    )
                )
            }
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (!isPluginGenerated(declaration)) return

        val levelName = declaration.name.asString()
        if (levelName !in LOG_LEVELS) return
        if (declaration.body != null) return // already has body

        val regularParams = declaration.parameters.filter {
            it.kind == IrParameterKind.Regular
        }
        val hasThrowable = regularParams.size == 2
        val parentClass = declaration.parent as? IrClass ?: return

        val logGetter = findLogGetter(parentClass) ?: return

        val isEnabledName =
            "is${levelName.replaceFirstChar { it.uppercase() }}Enabled"
        val isEnabledSymbol = context.referenceFunctions(
            CallableId(loggerClassId, Name.identifier(isEnabledName))
        ).first { symbol ->
            symbol.owner.parameters.none { it.kind == IrParameterKind.Regular }
        }

        val logMethodSymbol = context.referenceFunctions(
            CallableId(loggerClassId, Name.identifier(levelName))
        ).first { symbol ->
            val params = symbol.owner.parameters.filter {
                it.kind == IrParameterKind.Regular
            }
            if (hasThrowable) {
                params.size == 2 &&
                    params[1].type.classOrNull?.owner?.name?.asString() == "Throwable"
            } else {
                params.size == 1
            }
        }

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
                val logParams = call.symbol.owner.parameters.filter {
                    it.kind == IrParameterKind.Regular
                }
                call.arguments[logParams[0].indexInParameters] = msgExpr
                if (hasThrowable) {
                    call.arguments[logParams[1].indexInParameters] =
                        irGet(regularParams[0])
                }
            }

            +irIfThen(
                context.irBuiltIns.unitType,
                irCall(isEnabledSymbol).apply {
                    dispatchReceiver = irGet(logVal)
                },
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
