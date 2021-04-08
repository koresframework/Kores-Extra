/*
 *      Kores-Extra - Kores Extras
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 JonathanxD <https://github.com/JonathanxD/Kores-Extra>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.kores.extra

import com.github.jonathanxd.iutils.`object`.Default
import com.github.jonathanxd.iutils.`object`.Lazy
import com.github.jonathanxd.iutils.kt.*
import com.github.jonathanxd.iutils.opt.OptLazy
import com.github.jonathanxd.iutils.opt.OptObject
import com.github.jonathanxd.iutils.opt.specialized.*
import com.github.jonathanxd.iutils.reflection.Reflection
import com.github.jonathanxd.kores.type.`is`
import com.github.jonathanxd.kores.type.concreteType
import com.github.jonathanxd.kores.type.typeOf
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.*
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

/**
 * Validates [Opt] in [Method]
 */
fun Method.validateOpt() =
    if (this.isAnnotationPresent(classOf<Opt>()) && !this.returnType.isValidOpt())
        throw IllegalStateException("The return type of method '$this' annotated with 'Opt' must be an 'Optional'.")
    else Unit

fun Type.isValidOpt(): Boolean =
    this.apply { this.concreteType }.run {
        this.`is`(typeOf<Optional<*>>())
                || this.`is`(typeOf<OptionalInt>())
                || this.`is`(typeOf<OptionalDouble>())
                || this.`is`(typeOf<OptLazy<*>>())
                || this.`is`(typeOf<OptObject<*>>())
                || this.`is`(typeOf<OptShort>())
                || this.`is`(typeOf<OptByte>())
                || this.`is`(typeOf<OptChar>())
                || this.`is`(typeOf<OptInt>())
                || this.`is`(typeOf<OptBoolean>())
                || this.`is`(typeOf<OptFloat>())
                || this.`is`(typeOf<OptDouble>())
                || this.`is`(typeOf<OptLong>())
    }

fun Type.createSomeOpt(value: Any): Any =
    this.apply { this.concreteType }.run {
        when {
            this.`is`(typeOf<Optional<*>>()) -> Optional.of(value)
            this.`is`(typeOf<OptionalInt>()) -> OptionalInt.of(value as Int)
            this.`is`(typeOf<OptionalDouble>()) -> OptionalDouble.of(value as Double)
            this.`is`(typeOf<OptLazy<*>>()) -> someLazy(Lazy.evaluated(value))
            this.`is`(typeOf<OptObject<*>>()) -> some(value)
            this.`is`(typeOf<OptShort>()) -> someShort(value as Short)
            this.`is`(typeOf<OptByte>()) -> someByte(value as Byte)
            this.`is`(typeOf<OptChar>()) -> someChar(value as Char)
            this.`is`(typeOf<OptInt>()) -> someInt(value as Int)
            this.`is`(typeOf<OptBoolean>()) -> someBoolean(value as Boolean)
            this.`is`(typeOf<OptFloat>()) -> someFloat(value as Float)
            this.`is`(typeOf<OptDouble>()) -> someDouble(value as Double)
            this.`is`(typeOf<OptLong>()) -> someLong(value as Long)
            else -> value
        }
    }

fun Type.createNoneOpt(): Any =
    this.apply { this.concreteType }.run {
        when {
            this.`is`(typeOf<Optional<*>>()) -> Optional.empty<Any>()
            this.`is`(typeOf<OptionalInt>()) -> OptionalInt.empty()
            this.`is`(typeOf<OptionalDouble>()) -> OptionalDouble.empty()
            this.`is`(typeOf<OptLazy<*>>()) -> noneLazy<Any>()
            this.`is`(typeOf<OptObject<*>>()) -> none<Any>()
            this.`is`(typeOf<OptShort>()) -> noneShort()
            this.`is`(typeOf<OptByte>()) -> noneByte()
            this.`is`(typeOf<OptChar>()) -> noneChar()
            this.`is`(typeOf<OptInt>()) -> noneInt()
            this.`is`(typeOf<OptBoolean>()) -> noneBoolean()
            this.`is`(typeOf<OptFloat>()) -> noneFloat()
            this.`is`(typeOf<OptDouble>()) -> noneDouble()
            this.`is`(typeOf<OptLong>()) -> noneLong()
            else -> none<Any>()
        }
    }