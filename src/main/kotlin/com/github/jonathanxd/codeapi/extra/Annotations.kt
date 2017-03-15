/**
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
import com.github.jonathanxd.codeapi.base.impl.AnnotationImpl
import com.github.jonathanxd.codeapi.base.impl.EnumValueImpl
import com.github.jonathanxd.codeapi.type.CodeType
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.codeapi.util.toCodeType
import com.github.jonathanxd.iutils.array.ArrayUtils
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import javax.lang.model.element.*
import javax.lang.model.type.*

/**
 * Annotation universalizing.
 *
 * This class universalizes Java Reflection Annotations, Java model annotations and CodeAPI Annotations
 * via proxies.
 *
 * You need to have a interface that defines universalized annotation properties, example:
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
 * public interface EntryUniversalized {
 *
 *     CodeType type();
 *     String name();
 *
 * }
 * ```
 *
 * You only need to use `universal` version of annotation property type in the universalized interface.
 *
 * Each annotation property type has it own `universal` version:
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
 * public interface EntryUniversalized {
 *
 *     CodeType[] types();
 *     String name();
 *
 * }
 * ```
 *
 * Obs: you can also add a `annotationType` method in the universalizing interface.
 *
 *
 * ```java
 * public interface EntryUniversalized {
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
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> getUniversalInstance(annotation: Any, universalInterface: Class<T>): T {

    val codeAnnotation = when (annotation) {
        is kotlin.Annotation -> annotation.toCodeAPI()
        is AnnotationMirror -> annotation.toCodeAPI()
        is Annotation -> annotation
        else -> throw IllegalArgumentException("Unsupported annotation type: '${annotation::class.java.canonicalName}' (of instance '$annotation')")
    }

    return Proxy.newProxyInstance(universalInterface.classLoader, arrayOf(universalInterface), { proxy, method, args ->
        if (method.name == "annotationType")
            return@newProxyInstance codeAnnotation.type

        if (codeAnnotation.values.containsKey(method.name))
            return@newProxyInstance codeAnnotation.values[method.name]

        return@newProxyInstance method.invoke(annotation, *args)

    }) as T
}


private fun AnnotationMirror.toCodeAPI(): Annotation {
    val type = this.annotationType.toCodeType(false)

    val properties = this.elementValues.mapValues { (executableElement, annotationValue) ->
        annotationValue.toCodeAPIAnnotationValue(executableElement, executableElement.returnType)
    }.mapKeys { it.key.simpleName.toString() }

    return AnnotationImpl(type = type, values = properties, visible = true)
}

private fun AnnotationValue.toCodeAPIAnnotationValue(executableElement: ExecutableElement, type: TypeMirror): Any {
    val value = this.value ?: executableElement.defaultValue

    if (value is AnnotationMirror)
        return value.toCodeAPI()

    if (value is TypeMirror)
        return value.toCodeType(false)

    if (value is VariableElement)
        return EnumValueImpl(enumType = value.asType().toCodeType(false), enumEntry = value.simpleName.toString(), ordinal = -1)

    if (value is List<*>) {
        // Tries to convert to an reified array

        @Suppress("UNCHECKED_CAST")
        value as List<AnnotationValue>

        type as ArrayType

        val universal = type.componentType.universalType

        val newArray = java.lang.reflect.Array.newInstance(universal, value.size)

        value.forEachIndexed { index, arg ->
            java.lang.reflect.Array.set(newArray, index, arg.toCodeAPIAnnotationValue(executableElement, type.componentType))
        }

        return newArray
    }

    return value
}

private fun kotlin.Annotation.toCodeAPI(): Annotation {
    val type = this.annotationClass
    val jClass = this::class.java

    val properties = mutableMapOf<String, Any>()

    jClass.methods.forEach {
        if (it.declaringClass != Any::class.java) {
            if (Modifier.isPublic(it.modifiers) && it.parameterCount == 0) {
                properties.put(it.name, it.invoke(this).toCodeAPIAnnotationValue())
            }
        }
    }

    return AnnotationImpl(type = type.codeType, values = properties, visible = true)
}

private fun Any.toCodeAPIAnnotationValue(): Any {
    if (this is kotlin.Annotation)
        return this.toCodeAPI()

    if (this is Class<*>)
        return this.codeType

    if (this is Enum<*>)
        return EnumValueImpl(enumType = this::class.java.codeType, enumEntry = this.name, ordinal = this.ordinal)

    if (this::class.java.isArray) {

        val oldArray = ArrayUtils.toObjectArray(this)

        val array = this::class.java
        val componentType = array.componentType

        val newArray = java.lang.reflect.Array.newInstance(componentType.universalType, oldArray.size)

        oldArray.forEachIndexed { index, arg ->
            java.lang.reflect.Array.set(newArray, index, arg.toCodeAPIAnnotationValue())
        }

        return newArray
    }

    return this
}

private val Class<*>.universalType: Class<*>
    get() = when (this) {
        kotlin.Annotation::class.java -> Annotation::class.java
        Class::class.java -> CodeType::class.java
        else -> if(this.isEnum) EnumValue::class.java else this
    }

private val TypeMirror.universalType: Class<*>
    get() = when (this) {
        is DeclaredType -> when (this.asElement().kind) {
            ElementKind.ENUM -> EnumValue::class.java
            ElementKind.ANNOTATION_TYPE -> Annotation::class.java
            ElementKind.CLASS -> if (this.toString().startsWith("java.lang.Class")) CodeType::class.java
            else if (this.toString() == "java.lang.String") String::class.java
            else throw IllegalArgumentException("Cannot get universal type of type mirror '$this'")
            else -> throw IllegalArgumentException("Cannot get universal type of type mirror '$this'")
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
            else -> throw IllegalArgumentException("Cannot get universal type of type mirror '$this'")
        }
        else -> throw IllegalArgumentException("Cannot get universal type of type mirror '$this'")
    }

