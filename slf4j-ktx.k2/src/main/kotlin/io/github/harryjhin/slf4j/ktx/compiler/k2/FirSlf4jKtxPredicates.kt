package io.github.harryjhin.slf4j.ktx.compiler.k2

import io.github.harryjhin.slf4j.ktx.compiler.Slf4jKtxConfig
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate

/**
 * Predicate set matching the trigger annotation list held by [Slf4jKtxConfig].
 * Mirrors kotlinx-serialization's FirSerializationPredicates.
 *
 * Covers two arrival paths:
 *  - `annotated(*fqs)`      — the class carries a trigger annotation directly.
 *  - `metaAnnotated(*fqs)`  — one of the class's annotations is itself annotated with a trigger
 *                            (flat 1-hop — matches FirSerializationPredicates.hasMetaAnnotation).
 */
object FirSlf4jKtxPredicates {

    fun hasAnyTriggerAnnotation(config: Slf4jKtxConfig): DeclarationPredicate {
        val fqs = config.annotations.toTypedArray()
        return DeclarationPredicate.create {
            annotated(*fqs) or metaAnnotated(*fqs, includeItself = false)
        }
    }
}
