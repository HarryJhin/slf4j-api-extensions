package io.github.harryjhin.slf4j.ktx.compiler.backend

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxEntityNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Plugin-scoped IR context — delegates to the pristine [IrPluginContext] and adds:
 *  - [afterK2] flag so [isFromPlugin] can branch on origin family
 *  - lazily cached external symbols used by [Slf4jKtxIrGenerator]
 *
 * Mirrors kotlinx-serialization's `SerializationPluginContext`.
 */
class Slf4jKtxPluginContext(delegate: IrPluginContext) : IrPluginContext by delegate {

    // `afterK2: Boolean` is inherited from IrPluginContext directly — the compiler exposes it
    // even in 1.9.25. No override needed; callers use `ctx.afterK2` and get the delegate's value.

    val loggerClass: IrClassSymbol by lazy {
        referenceClass(Slf4jKtxEntityNames.LOGGER_CLASS_ID)
            ?: error("org.slf4j.Logger not on IR classpath")
    }

    val getLoggerSymbol: IrSimpleFunctionSymbol by lazy {
        val factory = referenceClass(Slf4jKtxEntityNames.LOGGER_FACTORY_CLASS_ID)
            ?: error("org.slf4j.LoggerFactory not on IR classpath")
        factory.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { fn ->
                fn.name == Slf4jKtxEntityNames.GET_LOGGER_NAME &&
                    fn.valueParameters.size == 1 &&
                    fn.valueParameters[0].type.classOrNull?.owner?.name?.asString() == "String"
            }
            .symbol
    }

    val function0InvokeSymbol: IrSimpleFunctionSymbol by lazy {
        val fn0 = referenceClass(ClassId(FqName("kotlin"), Name.identifier("Function0")))
            ?: error("kotlin.Function0 not on IR classpath")
        fn0.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == "invoke" }
            .symbol
    }
}
