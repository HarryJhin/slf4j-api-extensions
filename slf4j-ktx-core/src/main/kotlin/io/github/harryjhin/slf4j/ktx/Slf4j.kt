package io.github.harryjhin.slf4j.ktx

/**
 * Trigger annotation for the slf4j-ktx compiler plugin.
 *
 * Apply to a class (or `object`) to have the plugin inject:
 *  - a Companion object (if missing) containing
 *  - a `log: org.slf4j.Logger` property initialized with `LoggerFactory.getLogger(<class FQN>)`, and
 *  - `trace / debug / info / warn / error` × `(() -> String)` and `(Throwable, () -> String)`
 *    overloads whose bodies are level-guarded lazy invocations of the corresponding SLF4J method.
 *
 * Within the enclosing class body, those Companion members are accessible unqualified:
 *
 * ```
 * @Slf4j
 * class OrderService {
 *     fun process(order: Order) {
 *         info { "order: ${order.id}" }
 *         error(exception) { "failed" }
 *     }
 * }
 * ```
 *
 * Place on your own annotation to make it a compositional trigger (1-hop meta annotation):
 *
 * ```
 * @Slf4j annotation class LoggedStereotype
 * @LoggedStereotype class ReportGenerator { fun run() { info { "…" } } }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Slf4j
