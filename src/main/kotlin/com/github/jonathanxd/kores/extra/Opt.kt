package com.github.jonathanxd.kores.extra

import com.github.jonathanxd.iutils.`object`.Default
import com.github.jonathanxd.iutils.reflection.Reflection
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.typeOf
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Used to support [java.util.Optional] properties in unification. The [value] property defines the
 * evaluator that will returns whether to return [java.util.Optional.empty].
 *
 * Example:
 *
 * Annotation:
 * ```
 * annotation class Localization(val value: KClass<*> = Default::class.java)
 * ```
 *
 * Unification interface:
 *
 * ```
 * interface UnifiedLocalization {
 *     @Alias("value")
 *     @Opt
 *     fun getLocalization(): Optional<Class<*>>
 * }
 * ```
 *
 * For `@Localization`, `UnifiedLocalization.getLocalization()` returns `Optional.empty`.
 *
 * For `@Localization(String::class.java)`, `UnifiedLocalization.getLocalization()` returns
 * `Optional.of(String::class.java)`.
 *
 * @property value Evaluator that defines whether to return [java.util.Optional.empty] or not.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Opt(val value: KClass<out (Any) -> Boolean> = DefaultTypeCheck::class)

/**
 * Gets the checker
 */
@Suppress("UNCHECKED_CAST")
val Opt.checker: (Any) -> Boolean
    get() = Reflection.getInstance(this.value.java as Class<(Any) -> Boolean>)

/**
 * Checks if input value is [Default] type.
 */
object DefaultTypeCheck : (Any) -> Boolean {
    override fun invoke(p1: Any): Boolean = (p1 as? Type)?.`is`(typeOf<Default>()) == true
}

/**
 * Checks if list single value is [Default] type.
 */
object DefaultListTypeCheck : (Any) -> Boolean {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(p1: Any): Boolean =
        (p1 as? List<Any>)?.singleOrNull()?.let(DefaultTypeCheck) == true
}

/**
 * Checks if list is empty or single value is [Default] type.
 */
object EmptyOrDefaultListTypeCheck : (Any) -> Boolean {
    @Suppress("UNCHECKED_CAST")
    override fun invoke(p1: Any): Boolean =
        (p1 as? List<Any>)?.let { it.isEmpty() || DefaultListTypeCheck(it) } == true
}