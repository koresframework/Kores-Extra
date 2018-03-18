/*
 *      Kores-Extra - Kores Extras
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2018 JonathanxD <https://github.com/JonathanxD/Kores-Extra>
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

import com.github.jonathanxd.iutils.array.ArrayUtils
import com.github.jonathanxd.kores.base.Annotation
import com.github.jonathanxd.kores.base.EnumValue
import com.github.jonathanxd.kores.type.KoresType
import com.github.jonathanxd.kores.type.koresType
import com.github.jonathanxd.kores.type.toKoresType
import java.lang.reflect.*
import java.lang.reflect.Modifier
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.Elements

/**
 * Annotation systems unification.
 *
 * This function unifies Java Reflection Annotations, Java model annotations and Kores Annotations
 * via proxies.
 *
 * You need to have a interface that defines unification of annotation properties, example:
 *
 * Given:
 * ```java
 * public @interface Entry {
 *
 *     Class<?> type();
 *     String name();
 *
 * }
 * ```
 *
 * You need to write a interface like this:
 *
 * ```java
 * public interface EntryUnification {
 *
 *     KoresType type();
 *     String name();
 *
 * }
 * ```
 *
 * You only need to use `unification` version of annotation property type in the unification interface.
 *
 * Each annotation property type has it own `unified` version:
 *
 * - [Class] -> [KoresType]
 * - [Enum] -> [EnumValue]
 * - An [kotlin.Annotation] -> [Kores Annotation][Annotation]
 *
 * For arrays only change the component type, example:
 *
 * ```java
 * public @interface Entry {
 *     Class<?>[] types();
 *     String name();
 * }
 *
 *
 * public interface EntryUnification {
 *
 *     KoresType[] types();
 *     String name();
 *
 * }
 * ```
 *
 * Obs: you can also add a `annotationType` method in the unification interface,
 * this method returns the type of annotation (like [java.lang.annotation.Annotation.annotationType]).
 *
 *
 * ```java
 * public interface EntryUnification {
 *
 *     KoresType[] types();
 *     String name();
 *
 *     // Annotation type
 *     KoresType annotationType();
 *
 * }
 * ```
 *
 * You can also provide additional unification interfaces. Example:
 *
 * ```java
 *
 * public @interface Id {
 *   String value();
 * }
 *
 * public @interface Entry {
 *   Id id();
 *   Class<?> type();
 * }
 *
 * public interface IdUnification {
 *
 *   String value();
 *
 *   // Annotation type
 *   KoresType annotationType();
 *
 * }
 *
 * public interface EntryUnification {
 *
 *   KoresType type();
 *   IdUnification id();
 *
 *   // Annotation type
 *   KoresType annotationType();
 *
 * }
 * ```
 *
 * **Obs:** Since 1.2, Unification of [Javax Annotation Mirror][AnnotationMirror] requires a non-null [elements] instance,
 * if the [annotation] is an [AnnotationMirror], then [elements] **must not** be null, otherwise you can pass null.
 */
@JvmOverloads
fun <T : Any> getUnificationInstance(
    annotation: Any,
    unificationInterface: Class<T>,
    additionalUnificationGetter: (Type) -> Class<*>? = { null },
    elements: Elements? = null
): T {

    val unifiedAnnotation = getUnifiedAnnotationData(
        annotation,
        additionalUnificationGetter,
        elements
    )

    return createProxy(
        annotation,
        unificationInterface,
        unifiedAnnotation
    )
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> createProxy(
    annotationInstance: Any?,
    unificationInterface: Class<T>,
    unifiedAnnotationData: UnifiedAnnotationData
): T {

    return Proxy.newProxyInstance(
        unificationInterface.classLoader, arrayOf(unificationInterface),
        ProxyInvocationHandler(
            annotationInstance,
            unificationInterface,
            unifiedAnnotationData
        )
    ) as T
}

fun getHandlerOfAnnotation(proxy: Any) =
    (Proxy.getInvocationHandler(proxy) as? ProxyInvocationHandler
            ?: throw IllegalArgumentException("Provided 'proxy' class is not a UnifiedAnnotation"))


fun getDataOfAnnotation(proxy: Any) =
    getHandlerOfAnnotation(proxy).unifiedAnnotationData

fun getUnificationInterfaceOfAnnotation(proxy: Any) =
    getHandlerOfAnnotation(proxy).unificationInterface

fun getUnifiedAnnotationData(
    annotation: Any,
    additionalUnificationGetter: (Type) -> Class<*>? = { null },
    elements: Elements? = null
): UnifiedAnnotationData =
    when (annotation) {
        is kotlin.Annotation -> annotation.toUnified(additionalUnificationGetter, elements)
        is AnnotationMirror -> annotation.toUnified(
            additionalUnificationGetter,
            elements
                    ?: throw IllegalArgumentException("Since 1.2, Javax Annotation Mirror unification requires non-null 'elements' instance.")
        )
        is Annotation -> annotation.toUnified(additionalUnificationGetter, elements) // Remap values
        else -> throw IllegalArgumentException("Unsupported annotation type: '${annotation::class.java.canonicalName}' (of instance '$annotation')")
    }

private fun AnnotationMirror.toUnified(
    additionalUnificationGetter: (Type) -> Class<*>?,
    elements: Elements
): UnifiedAnnotationData {
    val type = this.annotationType.toKoresType(false, elements)

    val element = this.annotationType.asElement() as TypeElement

    require(element.kind == ElementKind.ANNOTATION_TYPE)

    val properties = mutableMapOf<String, Any>()

    element.enclosedElements.forEach {

        if (it is ExecutableElement) {
            val name = it.simpleName.toString()

            val annotationValue = this.elementValues[it] ?: it.defaultValue

            properties.put(
                name,
                annotationValue.toKoresAnnotationValue(
                    it,
                    it.returnType,
                    additionalUnificationGetter,
                    elements
                )
            )
        }


    }

    return UnifiedAnnotationData(type.koresType, properties)
}


private fun AnnotationValue.toKoresAnnotationValue(
    executableElement: ExecutableElement,
    type: TypeMirror,
    additionalUnificationGetter: (Type) -> Class<*>?,
    elements: Elements
): Any {
    val value = this.value ?: executableElement.defaultValue

    if (value is AnnotationMirror) {
        additionalUnificationGetter(value.annotationType.toKoresType(false, elements))?.let {
            return getUnificationInstance(
                this,
                it,
                additionalUnificationGetter,
                elements
            )
        }

        return value.toUnified(additionalUnificationGetter, elements)
    }

    if (value is TypeMirror)
        return value.toKoresType(false, elements)

    if (value is VariableElement)
        return EnumValue(
            enumType = value.asType().toKoresType(false, elements),
            enumEntry = value.simpleName.toString()
        )

    if (value is List<*>) {
        // Tries to convert to an reified array

        @Suppress("UNCHECKED_CAST")
        value as List<AnnotationValue>

        type as ArrayType

        return value.map {
            it.toKoresAnnotationValue(
                executableElement,
                type.componentType,
                additionalUnificationGetter,
                elements
            )
        }
    }

    return value
}

private fun kotlin.Annotation.toUnified(
    additionalUnificationGetter: (Type) -> Class<*>?,
    elements: Elements?
): UnifiedAnnotationData {
    val type = this.annotationClass
    val jClass = this::class.java

    val properties = mutableMapOf<String, Any>()

    jClass.methods.forEach {
        if (it.declaringClass != Any::class.java) {
            if (Modifier.isPublic(it.modifiers) && it.parameterCount == 0) {
                properties.put(
                    it.name, it.invoke(this).toKoresAnnotationValue(
                        it.returnType, additionalUnificationGetter,
                        elements
                    )
                )
            }
        }
    }

    return UnifiedAnnotationData(type.koresType, properties)
}

private fun Any.toKoresAnnotationValue(
    rType: Type,
    additionalUnificationGetter: (Type) -> Class<*>?,
    elements: Elements?
): Any {
    if (this is kotlin.Annotation) {
        additionalUnificationGetter(this.annotationClass.koresType)?.let {
            return getUnificationInstance(
                this,
                it,
                additionalUnificationGetter,
                elements
            )
        }

        return this.toUnified(additionalUnificationGetter, elements)
    }

    if (this is Annotation) {
        additionalUnificationGetter(this.type)?.let {
            return getUnificationInstance(
                this,
                it,
                additionalUnificationGetter,
                elements
            )
        }

        return this.toUnified(additionalUnificationGetter, elements)
    }

    if (this is Class<*>)
        return this.koresType

    if (this is Enum<*>)
        return EnumValue(enumType = rType, enumEntry = this.name)

    if (this::class.java.isArray) {

        val oldArray = ArrayUtils.toObjectArray(this)

        val array = this::class.java
        val componentType = array.componentType

        return oldArray.map {
            it.toKoresAnnotationValue(componentType, additionalUnificationGetter, elements)
        }

    }

    if (this is List<*>) {
        return this.filterNotNull().forEach {
            it.toKoresAnnotationValue(rType, additionalUnificationGetter, elements)
        }
    }

    return this
}

private fun Annotation.toUnified(
    additionalUnificationGetter: (Type) -> Class<*>?,
    elements: Elements?
): UnifiedAnnotationData {
    val type = this.type

    val properties = this.values.mapValues {
        return@mapValues if (it.value is Annotation) {
            val annotation = it.value as Annotation
            val get = additionalUnificationGetter(annotation.type)

            if (get != null) {
                getUnificationInstance(
                    annotation,
                    get,
                    additionalUnificationGetter,
                    elements
                )
            } else it.value

        } else if (it.value::class.java.isArray) {
            val oldArray = ArrayUtils.toObjectArray(it.value)

            val arrayComp = it.value::class.java.componentType

            oldArray.map {
                it.toKoresAnnotationValue(arrayComp, additionalUnificationGetter, elements)
            }
        } else if (it.value is List<*> && (it.value as List<*>).filterNotNull().all { it is Annotation }) {
            (it.value as List<*>).filterNotNull().map { it as Annotation }.map {
                it.toKoresAnnotationValue(it.type, additionalUnificationGetter, elements)
            }
        } else {
            it.value
        }

    }

    return UnifiedAnnotationData(type.koresType, properties)
}


private val Class<*>.unificationType: Class<*>
    get() = when (this) {
        kotlin.Annotation::class.java -> Annotation::class.java
        Class::class.java -> KoresType::class.java
        else -> if (this.isEnum) EnumValue::class.java else this
    }

private val TypeMirror.unificationType: Class<*>
    get() = when (this) {
        is DeclaredType -> when (this.asElement().kind) {
            ElementKind.ENUM -> EnumValue::class.java
            ElementKind.ANNOTATION_TYPE -> Annotation::class.java
            ElementKind.CLASS -> if (this.toString().startsWith("java.lang.Class")) KoresType::class.java
            else if (this.toString() == "java.lang.String") String::class.java
            else throw IllegalArgumentException("Cannot get unification type of type mirror '$this'")
            else -> throw IllegalArgumentException("Cannot get unification type of type mirror '$this'")
        }
        is PrimitiveType -> when (this.kind) {
            TypeKind.BOOLEAN -> Boolean::class.javaPrimitiveType!!
            TypeKind.BYTE -> Byte::class.javaPrimitiveType!!
            TypeKind.SHORT -> Short::class.javaPrimitiveType!!
            TypeKind.INT -> Int::class.javaPrimitiveType!!
            TypeKind.FLOAT -> Float::class.javaPrimitiveType!!
            TypeKind.DOUBLE -> Double::class.javaPrimitiveType!!
            TypeKind.LONG -> Long::class.javaPrimitiveType!!
            TypeKind.VOID -> Void::class.javaPrimitiveType!!
            else -> throw IllegalArgumentException("Cannot get unification type of type mirror '$this'")
        }
        else -> throw IllegalArgumentException("Cannot get unification type of type mirror '$this'")
    }


class UnifiedAnnotationData(val type: KoresType, values_: Map<String, Any>) {
    val values: Map<String, Any> = Collections.unmodifiableMap(values_)
}

class ProxyInvocationHandler(
    val original: Any?,
    val unificationInterface: Class<*>,
    val unifiedAnnotationData: UnifiedAnnotationData
) : InvocationHandler {

    private val nameMappings = mutableMapOf<String, String>()

    init {
        unificationInterface.methods.forEach { method ->
            method.getDeclaredAnnotation(Alias::class.java)?.value?.also { alias ->
                nameMappings[method.name] = alias
            }
            Unit
        }
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val mname_ = method.name

        val name = if (nameMappings.containsKey(mname_)) nameMappings[mname_]!! else mname_

        if (name == "annotationType")
            return unifiedAnnotationData.type

        if (proxy is UnifiedAnnotation) {
            if (name == "getUnifiedAnnotationData")
                return getDataOfAnnotation(proxy)
            if (name == "getUnifiedAnnotationOrigin")
                return original ?: Unit
        }

        if (unifiedAnnotationData.values.containsKey(name)) {

            val value = unifiedAnnotationData.values[name]

            if (value != null && value::class.java.isArray && method.returnType.isArray) {
                if (java.lang.reflect.Array.getLength(value) == 0)
                    return java.lang.reflect.Array.newInstance(method.returnType.componentType, 0)

                val oldArray = ArrayUtils.toObjectArray(value)

                val array = method.returnType
                val componentType = array.componentType

                val newArray = java.lang.reflect.Array.newInstance(componentType, oldArray.size)

                oldArray.forEachIndexed { index, arg ->
                    java.lang.reflect.Array.set(newArray, index, arg)
                }

                return newArray
            }

            return value
                    ?: throw NullPointerException("Annotation properties should not return null! (at: $method)")

        }


        return try {
            original?.let {
                (if (args != null) method.invoke(it, *args) else method.invoke(it))
                        ?: throw NullPointerException("Annotation properties should not return null! (at: $method)")
            } ?: throw NoSuchMethodError("Method not found '$method'!")
        } catch (t: Throwable) {
            throw ReflectiveOperationException("Method not found '$method' at $original!", t)
        }
    }

}