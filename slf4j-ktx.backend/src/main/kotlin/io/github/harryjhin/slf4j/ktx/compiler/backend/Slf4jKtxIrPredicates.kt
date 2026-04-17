package io.github.harryjhin.slf4j.ktx.compiler.backend

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxPluginKey
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin

/**
 * Identifies plugin-generated IR declarations under both K1 and K2 origins.
 *
 * - K2 path: FIR marks our declarations with `IrDeclarationOrigin.GeneratedByPlugin(key)`.
 * - K1 path: descriptor's CallableMemberDescriptor.Kind is SYNTHESIZED.
 *
 * Mirrors kotlinx-serialization's `IrPredicates.isFromPlugin(afterK2)` contract.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrDeclaration.isFromPlugin(afterK2: Boolean): Boolean =
    if (afterK2) {
        origin == IrDeclarationOrigin.GeneratedByPlugin(Slf4jKtxPluginKey)
    } else {
        (descriptor as? CallableMemberDescriptor)?.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
    }
