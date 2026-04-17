package io.github.harryjhin.slf4j.ktx

/**
 * Trigger annotation for the slf4j-ktx compiler plugin.
 *
 * When present on a class or `object`, the plugin:
 *  - ensures the target has a Companion (auto-generates one on a plain class if absent; uses
 *    the `object` itself when applied to an `object`);
 *  - synthesizes a single `log: org.slf4j.Logger` property on that site, initialized with
 *    `LoggerFactory.getLogger(<enclosing class FQN>)`;
 *  - rewrites every call to the runtime `T.trace / T.debug / T.info / T.warn / T.error`
 *    extensions (declared in this module, see `LoggerExtensions.kt`) made from inside a
 *    triggered class into a direct static-field access against the Companion's `log`. The
 *    rewrite runs at IR-lowering time and skips `LoggerFactory.getLogger` entirely on the
 *    hot path.
 *
 * Classes without `@Slf4j` (or a custom trigger annotation) still compile and can call the
 * same extensions — they just go through the extensions' default body, which does a single
 * SLF4J cache lookup (`LoggerFactory.getLogger(T::class.java)`). Correctness is preserved;
 * only the microbenchmark-level peak performance of a direct static-field read is sacrificed.
 *
 * Typical usage — add `@Slf4j` and call the level extensions directly (with wildcard import
 * from this package so the extensions are in scope):
 *
 * ```
 * import io.github.harryjhin.slf4j.ktx.*
 *
 * @Slf4j
 * class OrderService {
 *     fun process(order: Order) {
 *         info { "order: ${order.id}" }
 *         error(exception) { "failed" }
 *     }
 * }
 * ```
 *
 * **Meta-annotation (1-hop).** Place `@Slf4j` on your own annotation to turn it into a
 * compositional trigger:
 *
 * ```
 * @Slf4j annotation class LoggedStereotype
 *
 * @LoggedStereotype
 * class ReportGenerator {
 *     fun run() { info { "…" } }
 * }
 * ```
 *
 * The predicate is single-hop: only annotations directly marked with `@Slf4j` qualify.
 * `@Slf4j → @A → @B → target-class` does **not** propagate through `@A`.
 *
 * **Spring integration.** Apply the Gradle plugin `id("io.github.harryjhin.slf4j-ktx.spring")`
 * to have the six Spring stereotype FQNs (`@Component` / `@Controller` / `@Service` /
 * `@Repository` / `@RestController` / `@ControllerAdvice`) registered as additional triggers
 * alongside `@Slf4j`. Custom triggers can be added via `slf4jKtx { annotation("…") }`.
 *
 * **Visibility.** The injected `log` property is `internal`. Intra-module code can reach it
 * for parameterized-message APIs the runtime extensions don't cover (e.g. `log.info("{}", x)`);
 * external modules must annotate their own declarations rather than fetch another class's
 * logger. Same Lombok-`@Slf4j` semantics.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Slf4j
