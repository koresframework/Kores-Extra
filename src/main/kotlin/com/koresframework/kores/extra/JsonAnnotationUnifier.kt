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
package com.koresframework.kores.extra

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import com.koresframework.kores.base.Annotation
import com.koresframework.kores.base.EnumValue
import com.koresframework.kores.util.*
import com.github.jonathanxd.iutils.kt.rightOrFail
import com.koresframework.kores.base.Retention
import com.koresframework.kores.type.*
import java.lang.reflect.Type
import javax.lang.model.element.ExecutableElement
import javax.lang.model.util.Elements

typealias TypeResolverFunc = (annotationType: Type, name: String) -> UnificationStrategy?

private val GLOBAL_OM = ObjectMapper()

@Suppress("UNCHECKED_CAST")
fun <T : Any> unifyJson(jsonString: String,
                        baseAnnotationType: Type,
                        unificationInterface: Class<T>,
                        typeResolver: TypeResolverFunc
): T = unifyJson(jsonString, baseAnnotationType, unificationInterface, typeResolver, GLOBAL_OM)

@Suppress("UNCHECKED_CAST")
fun <T : Any> unifyJson(jsonString: String,
                        baseAnnotationType: Type,
                        unificationInterface: Class<T>,
                        typeResolver: TypeResolverFunc,
                        om: ObjectMapper
): T {
    val json = om.readTree(jsonString) as ObjectNode

    return unifyJsonObj(
        json,
        baseAnnotationType,
        unificationInterface,
        typeResolver
    )
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> unifyJsonObj(jsonObject: ObjectNode,
                           baseAnnotationType: Type,
                           unificationInterface: Class<T>,
                           typeResolver: TypeResolverFunc
): T {

    val annotation = jsonObject.fromJsonObjAnn(baseAnnotationType, typeResolver)

    return createProxy(
        jsonObject,
        unificationInterface,
        UnifiedAnnotationData(
            annotation.type.koresType,
            annotation.values
        )
    )
}

private fun ObjectNode.fromJsonObjAnn(type: Type, typeResolver: TypeResolverFunc): Annotation {
    val map = mutableMapOf<String, Any>()

    this.fields().forEach {
        val key = it.key as String
        val value = it.value as JsonNode

        map[key] = fromJsonVal(type, key, value, typeResolver)
    }

    return Annotation.Builder.builder()
            .type(type)
            .retention(Retention.RUNTIME)
            .values(map)
            .build()
}

private fun fromJsonObj(annotationType: Type, name: String, json: ObjectNode, typeResolver: TypeResolverFunc): Any {
    val resolved = typeResolver(annotationType, name)

    return when (resolved) {
        is AnnotationConstant -> json.fromJsonObjAnn(resolved.type, typeResolver)
        is UnifyAnnotation -> unifyJsonObj(
            json,
            resolved.annotationType,
            resolved.unificationType,
            typeResolver
        )
        else -> throw IllegalArgumentException("No value resolution provided for object: $json of key: $name")
    }

}

private fun fromResolved(annotationType: Type, strategy: UnificationStrategy, value: JsonNode, typeResolver: TypeResolverFunc): Any =
        when (strategy) {
            is EnumConstant -> EnumValue(strategy.type, value.textValue())
            is AnnotationConstant -> (value as ObjectNode).fromJsonObjAnn(strategy.type, typeResolver)
            is UnifyAnnotation -> unifyJsonObj(
                value as ObjectNode,
                strategy.annotationType,
                strategy.unificationType,
                typeResolver
            )
            is LiteralValue -> value.toJavaConstant().toConstValue(strategy.type)
            is Apply -> strategy(value.toJavaConstant())
        }

private fun fromJsonVal(annotationType: Type, key: String, value: JsonNode, typeResolver: TypeResolverFunc): Any {

    val resolved = typeResolver(annotationType, key)

    return when (value) {
        is TextNode, is NumericNode, is FloatNode, is Number,
        is BooleanNode -> if (resolved != null) fromResolved(
            annotationType,
            resolved,
            value,
            typeResolver
        ) else value.toJavaConstant()
        is ArrayNode -> value.map {
            if (resolved != null) fromResolved(
                annotationType,
                resolved,
                it,
                typeResolver
            )
            else fromJsonVal(
                annotationType,
                key,
                it,
                typeResolver
            )
        }
        is ObjectNode -> {
            if (resolved != null) fromResolved(
                annotationType,
                resolved,
                value,
                typeResolver
            )
            else fromJsonObj(
                annotationType,
                key,
                value,
                typeResolver
            )
        }
    /*is Map<*, *> -> value.map { (ka, va) ->
        ka as String
        va as Any
        fromJsonVal(annotationType, ka, va, typeResolver)
    }*/
        else -> throw IllegalArgumentException("Invalid of value of key($key). Value: $value.")
    }

}

fun JsonNode.toJavaConstant(): Any =
    when (this) {
        is TextNode -> this.textValue()
        is DoubleNode -> this.doubleValue()
        is FloatNode -> this.floatValue()
        is ShortNode -> this.shortValue()
        is IntNode -> this.intValue()
        is LongNode -> this.longValue()
        is DecimalNode -> this.doubleValue()
        is BigIntegerNode -> this.bigIntegerValue()
        else -> throw IllegalArgumentException("Unknown node kind: ${this::class.java.simpleName}. Value: $this")
    }

private fun Any.toConstValue(type: LiteralType): Any {
    return type.converter(this)
}

sealed class UnificationStrategy

class Apply(val func: (Any) -> Any): UnificationStrategy() {
    operator fun invoke(any: Any): Any = func(any)
}

class LiteralValue(val type: LiteralType) : UnificationStrategy()
class EnumConstant(val type: Type) : UnificationStrategy()
class AnnotationConstant(val type: Type) : UnificationStrategy()
class UnifyAnnotation(val annotationType: Type, val unificationType: Class<*>) : UnificationStrategy()

enum class LiteralType(val type: Class<*>, val converter: (Any) -> Any) {
    BOOLEAN(Boolean::class.javaPrimitiveType!!, { it.toString().toBoolean() }),
    CHAR(Char::class.javaPrimitiveType!!, { it.toString().toCharArray().single() }),
    BYTE(Byte::class.javaPrimitiveType!!, { it.toString().toByte() }),
    SHORT(Short::class.javaPrimitiveType!!, { it.toString().toShort() }),
    INT(Int::class.javaPrimitiveType!!, { it.toString().toInt() }),
    FLOAT(Float::class.javaPrimitiveType!!, { it.toString().toFloat() }),
    LONG(Long::class.javaPrimitiveType!!, { it.toString().toLong() }),
    DOUBLE(Double::class.javaPrimitiveType!!, { it.toString().toDouble() }),
    STRING(String::class.java, { it.toString() }),
    TYPE(Type::class.java, { it })
}

class JavaAnnotationResolverFunc(val additionalUnificationGetter: (Type) -> Class<*>? = { null },
                                 val koresTypeResolverFunc: KoresTypeResolverFunc,
                                 loader: ClassLoader?) :
    TypeResolverFunc {

    val jResolver = KoresTypeResolver.Java(loader ?: ClassLoader.getSystemClassLoader())

    override fun invoke(annotationType: Type, name: String): UnificationStrategy? {
        val cType = (if(annotationType.isArray) annotationType.arrayBaseComponent else annotationType).concreteType

        val resolvedClass = this.jResolver.resolve(cType).rightOrFail

        try {
            val prop = resolvedClass.getDeclaredMethod(name)
            val rType = prop.returnType.baseComponent()

            if (rType.isAnnotation) {

                return additionalUnificationGetter(rType)?.let {
                    UnifyAnnotation(
                        rType,
                        it
                    )
                }
                        ?: AnnotationConstant(rType)
            }

            if (rType.isEnum) {
                return EnumConstant(rType)
            }

            if (rType.`is`(Class::class.java)) {
                return Apply { koresTypeResolverFunc(it.toString()) }
            }

            LiteralType.values().firstOrNull { it.type == rType }?.let {
                return LiteralValue(it)
            }
        } catch (e: Exception) {
            throw IllegalStateException("Cannot find property '$name' in annotation '$annotationType'", e)
        }

        return null
    }

}

class ModelAnnotationResolverFunc(val additionalUnificationGetter: (Type) -> Class<*>? = { null },
                                  val elements: Elements) :
    TypeResolverFunc {

    val mResolver = KoresTypeResolver.Model(elements)

    override fun invoke(annotationType: Type, name: String): UnificationStrategy? {
        val cType = annotationType.arrayBaseComponent.concreteType

        val resolvedType = this.mResolver.resolve(cType).rightOrFail

        try {
            val elems = resolvedType.enclosedElements.filterIsInstance<ExecutableElement>()

            val prop = elems.first { it.simpleName.contentEquals(name) }
            val rType = prop.returnType.getKoresType(elements).arrayBaseComponent

            val isAnnotation = this.mResolver
                    .isAssignableFrom(kotlin.Annotation::class.java, rType, {mResolver}).rightOrFail

            val isEnum = this.mResolver
                    .isAssignableFrom(kotlin.Enum::class.java, rType, {mResolver}).rightOrFail

            if (isAnnotation) {
                return additionalUnificationGetter(rType)?.let {
                    UnifyAnnotation(
                        rType,
                        it
                    )
                }
                        ?: AnnotationConstant(rType)
            }

            if (isEnum) {
                return EnumConstant(rType)
            }

            if (rType.`is`(Class::class.java)) {
                return Apply {
                    elements.getTypeElement(it.toString()).getKoresType(elements)
                }
            }

            LiteralType.values().firstOrNull { it.type.`is`(rType) }?.let {
                return LiteralValue(it)
            }
        } catch (e: Exception) {
            throw IllegalStateException("Cannot find property '$name' in annotation '$annotationType'", e)
        }

        return null
    }

}


tailrec fun Class<*>.baseComponent(): Class<*> =
        if (!this.isArray) this
        else this.componentType.baseComponent()
