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

import com.github.jonathanxd.codeapi.base.Annotation
import com.github.jonathanxd.codeapi.base.EnumValue
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.toCodeType
import com.github.jonathanxd.iutils.array.ArrayUtils
import java.lang.reflect.*
import java.lang.reflect.Modifier
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.Elements

/**
 * Annotation systems unification.
 *
 * This function unifies Java Reflection Annotations, Java model annotations and CodeAPI Annotations
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
 *     CodeType type();
 *     String name();
 *
 * }
 * ```
 *
 * You only need to use `unification` version of annotation property type in the unification interface.
 *
 * Each annotation property type has it own `unified` version:
 *
 * - [Class] -> [CodeType]
 * - [Enum] -> [EnumValue]
 * - An [kotlin.Annotation] -> [Code API Annotation][Annotation]
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
 *     CodeType[] types();
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
 *     CodeType[] types();
 *     String name();
 *
 *     // Annotation type
 *     CodeType annotationType();
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
 *   CodeType annotationType();
 *
 * }
 *
 * public interface EntryUnification {
 *
 *   CodeType type();
 *   IdUnification id();
 *
 *   // Annotation type
 *   CodeType annotationType();
 *
 * }
 * ```
 *
 * **Obs:** Since 1.2, Unification of [Javax Annotation Mirror][AnnotationMirror] requires a non-null [elements] instance,
 * if the [annotation] is an [AnnotationMirror], then [elements] **must not** be null, otherwise you can pass null.
 */
@JvmOverloads
fun <T : Any> getUnificationInstance(annotation: Any,
                                     unificationInterface: Class<T>,
                                     additionalUnificationGetter: (Type) -> Class<*>? = { null },
                                     elements: Elements? = null): T {

    val unifiedAnnotation = getUnifiedAnnotationData(annotation, additionalUnificationGetter, elements)

    return createProxy(annotation, unificationInterface, unifiedAnnotation)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> createProxy(annotationInstance: Any?,
                          unificationInterface: Class<T>,
                          unifiedAnnotationData: UnifiedAnnotationData): T {

    return Proxy.newProxyInstance(unificationInterface.classLoader, arrayOf(unificationInterface),
            ProxyInvocationHandler(annotationInstance, unificationInterface, unifiedAnnotationData)) as T
}

fun getHandlerOfAnnotation(proxy: Any) =
        (Proxy.getInvocationHandler(proxy) as? ProxyInvocationHandler
                ?: throw IllegalArgumentException("Provided 'proxy' class is not a UnifiedAnnotation"))


fun getDataOfAnnotation(proxy: Any) =
        getHandlerOfAnnotation(proxy).unifiedAnnotationData

fun getUnificationInterfaceOfAnnotation(proxy: Any) =
        getHandlerOfAnnotation(proxy).unificationInterface

fun getUnifiedAnnotationData(annotation: Any,
                             additionalUnificationGetter: (Type) -> Class<*>? = { null },
                             elements: Elements? = null): UnifiedAnnotationData =
        when (annotation) {
            is kotlin.Annotation -> annotation.toUnified(additionalUnificationGetter, elements)
            is AnnotationMirror -> annotation.toUnified(additionalUnificationGetter, elements ?: throw IllegalArgumentException("Since 1.2, Javax Annotation Mirror unification requires non-null 'elements' instance."))
            is Annotation -> annotation.toUnified(additionalUnificationGetter, elements) // Remap values
            else -> throw IllegalArgumentException("Unsupported annotation type: '${annotation::class.java.canonicalName}' (of instance '$annotation')")
        }

private fun AnnotationMirror.toUnified(additionalUnificationGetter: (Type) -> Class<*>?, elements: Elements): UnifiedAnnotationData {
    val type = this.annotationType.toCodeType(false, elements)

    val element = this.annotationType.asElement() as TypeElement

    require(element.kind == ElementKind.ANNOTATION_TYPE)

    val properties = mutableMapOf<String, Any>()

    element.enclosedElements.forEach {

        if (it is ExecutableElement) {
            val name = it.simpleName.toString()

            val annotationValue = this.elementValues[it] ?: it.defaultValue

            properties.put(name, annotationValue.toCodeAPIAnnotationValue(it, it.returnType, additionalUnificationGetter, elements))
        }


    }

    return UnifiedAnnotationData(type.codeType, properties)
}


private fun AnnotationValue.toCodeAPIAnnotationValue(executableElement: ExecutableElement,
                                                     type: TypeMirror,
                                                     additionalUnificationGetter: (Type) -> Class<*>?,
                                                     elements: Elements): Any {
    val value = this.value ?: executableElement.defaultValue

    if (value is AnnotationMirror) {
        additionalUnificationGetter(value.annotationType.toCodeType(false, elements))?.let {
            return getUnificationInstance(this, it, additionalUnificationGetter, elements)
        }

        return value.toUnified(additionalUnificationGetter, elements)
    }

    if (value is TypeMirror)
        return value.toCodeType(false, elements)

    if (value is VariableElement)
        return EnumValue(
                enumType = value.asType().toCodeType(false, elements),
                enumEntry = value.simpleName.toString(),
                ordinal = -1
        )

    if (value is List<*>) {
        // Tries to convert to an reified array

        @Suppress("UNCHECKED_CAST")
        value as List<AnnotationValue>

        type as ArrayType

        val unification = type.componentType.unificationType

        val newArray = java.lang.reflect.Array.newInstance(unification, value.size)

        value.forEachIndexed { index, arg ->
            java.lang.reflect.Array.set(newArray, index, arg.toCodeAPIAnnotationValue(executableElement, type.componentType, additionalUnificationGetter, elements))
        }

        return newArray
    }

    return value
}

private fun kotlin.Annotation.toUnified(additionalUnificationGetter: (Type) -> Class<*>?, elements: Elements?): UnifiedAnnotationData {
    val type = this.annotationClass
    val jClass = this::class.java

    val properties = mutableMapOf<String, Any>()

    jClass.methods.forEach {
        if (it.declaringClass != Any::class.java) {
            if (Modifier.isPublic(it.modifiers) && it.parameterCount == 0) {
                properties.put(it.name, it.invoke(this).toCodeAPIAnnotationValue(additionalUnificationGetter, elements))
            }
        }
    }

    return UnifiedAnnotationData(type.codeType, properties)
}

private fun Any.toCodeAPIAnnotationValue(additionalUnificationGetter: (Type) -> Class<*>?, elements: Elements?): Any {
    if (this is kotlin.Annotation) {
        additionalUnificationGetter(this.annotationClass.codeType)?.let {
            return getUnificationInstance(this, it, additionalUnificationGetter, elements)
        }

        return this.toUnified(additionalUnificationGetter, elements)
    }

    if (this is Class<*>)
        return this.codeType

    if (this is Enum<*>)
        return EnumValue(enumType = this::class.java.codeType, enumEntry = this.name, ordinal = this.ordinal)

    if (this::class.java.isArray) {

        val oldArray = ArrayUtils.toObjectArray(this)

        val array = this::class.java
        val componentType = array.componentType

        val newArray = java.lang.reflect.Array.newInstance(componentType.unificationType, oldArray.size)

        oldArray.forEachIndexed { index, arg ->
            java.lang.reflect.Array.set(newArray, index, arg.toCodeAPIAnnotationValue(additionalUnificationGetter, elements))
        }

        return newArray
    }

    return this
}

private fun Annotation.toUnified(additionalUnificationGetter: (Type) -> Class<*>?, elements: Elements?): UnifiedAnnotationData {
    val type = this.type

    val properties = this.values.mapValues {
        return@mapValues if (it.value is Annotation) {
            val annotation = it.value as Annotation
            val get = additionalUnificationGetter(annotation.type)

            if (get != null) {
                getUnificationInstance(annotation, get, additionalUnificationGetter, elements)
            } else it.value

        } else it.value

    }

    return UnifiedAnnotationData(type.codeType, properties)
}


private val Class<*>.unificationType: Class<*>
    get() = when (this) {
        kotlin.Annotation::class.java -> Annotation::class.java
        Class::class.java -> CodeType::class.java
        else -> if (this.isEnum) EnumValue::class.java else this
    }

private val TypeMirror.unificationType: Class<*>
    get() = when (this) {
        is DeclaredType -> when (this.asElement().kind) {
            ElementKind.ENUM -> EnumValue::class.java
            ElementKind.ANNOTATION_TYPE -> Annotation::class.java
            ElementKind.CLASS -> if (this.toString().startsWith("java.lang.Class")) CodeType::class.java
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


class UnifiedAnnotationData(val type: CodeType, values_: Map<String, Any>) {
    val values: Map<String, Any> = Collections.unmodifiableMap(values_)
}

class ProxyInvocationHandler(val original: Any?,
                             val unificationInterface: Class<*>,
                             val unifiedAnnotationData: UnifiedAnnotationData) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        if (method.name == "annotationType")
            return unifiedAnnotationData.type

        if (proxy is UnifiedAnnotation) {
            if (method.name == "getUnifiedAnnotationData")
                return getDataOfAnnotation(proxy)
        }

        if (unifiedAnnotationData.values.containsKey(method.name)) {
            return unifiedAnnotationData.values[method.name]
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