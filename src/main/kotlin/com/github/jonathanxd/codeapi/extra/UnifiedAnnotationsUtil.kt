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

import com.github.jonathanxd.codeapi.type.CodeType

/**
 * Map unified annotation [T] value map and create a new instance of unified annotation of type [T]
 * with modified value map.
 */
@Suppress("UNCHECKED_CAST")
@JvmOverloads
inline fun <T : Any> map(annotation: T,
                         mapper: (MutableMap<String, Any>) -> Unit,
                         propertyMapper: ((name: String, value: Any?, annotationType: CodeType) -> Any)? = null): T {
    val handler = getHandlerOfAnnotation(annotation)
    val unifiedAnnotationData = handler.unifiedAnnotationData
    val map = HashMap(unifiedAnnotationData.values)

    mapper(map)

    return createProxy(annotation, handler.unificationInterface as Class<T>,
            UnifiedAnnotationData(unifiedAnnotationData.type, map), propertyMapper ?: handler.propertyMapper)
}

