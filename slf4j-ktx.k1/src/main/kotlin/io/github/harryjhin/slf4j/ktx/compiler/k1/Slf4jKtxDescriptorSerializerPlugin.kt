package io.github.harryjhin.slf4j.ktx.compiler.k1

import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin

/**
 * Empty descriptor-metadata hook registered to mirror kotlinx-serialization's
 * SerializationDescriptorSerializerPlugin. Reserved as a filter slot for the phantom-Companion
 * scenario: if Step-5 output (auto-generated Companion objects) appears as visible nested classes
 * in downstream modules' `.kotlin_metadata`, override here to filter them out.
 *
 * Verification of that phantom scenario is deferred to PLAN Appendix A (kotlinp dump).
 */
class Slf4jKtxDescriptorSerializerPlugin : DescriptorSerializerPlugin
