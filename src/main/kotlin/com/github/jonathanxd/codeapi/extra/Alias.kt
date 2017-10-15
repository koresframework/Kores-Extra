/*
 *      CodeAPI-Extra - CodeAPI Extras
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 JonathanxD <https://github.com/JonathanxD/CodeAPI-Extra>
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
package com.github.jonathanxd.codeapi.extra

/**
 * Defines the alias of the method. This alias is the name of the method in the annotations to unify. This allows
 * more conventional names for interface methods rather than naming conventions used in annotations, so instead
 * of naming a method in this way: `age`, you can name it as `getAge` and add [Alias] annotation with `age` value.
 *
 * Example:
 *
 * Annotation:
 * ```
 * annotation class Person(val name: String, val age: Int)
 * ```
 *
 * Unification interface:
 *
 * ```
 * interface UnifiedPerson {
 *     @Alias("name")
 *     fun getName(): String
 *     @Alias("age")
 *     fun getAge(): Int
 * }
 * ```
 *
 * We can also use Kotlin properties, but alias still required because Kotlin properties are compiled to getters methods
 * (and setters if property is mutable, but this is irrelevant for unification).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Alias(val value: String)