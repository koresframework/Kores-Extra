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
package com.github.jonathanxd.codeapi.extra.test;

import com.google.common.truth.FailureStrategy;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.EnumValue;
import com.github.jonathanxd.codeapi.base.impl.EnumValueImpl;
import com.github.jonathanxd.codeapi.extra.AnnotationsKt;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.PlainCodeType;
import com.github.jonathanxd.iutils.collection.CollectionUtils;
import com.github.jonathanxd.iutils.container.primitivecontainers.IntContainer;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

public class AnnotationUniversalTest {

    @Entry(name = "a", entryTypes = {Type.REGISTER, Type.LOG}, ids = {0, 1, 2}, flag = 0, types = {String.class, CharSequence.class})
    public static final String a = "0";

    @Test
    public void test() throws Exception {

        CodeType TYPE_TYPE = CodeAPI.getJavaType(Type.class);

        Annotation annotation = AnnotationUniversalTest.class.getField("a").getDeclaredAnnotation(Entry.class);

        UniversalEntry universalInstance = AnnotationsKt.getUniversalInstance(annotation, UniversalEntry.class);

        Assert.assertEquals("a", universalInstance.name());
        Assert.assertArrayEquals(new int[]{0, 1, 2}, universalInstance.ids());
        Assert.assertArrayEquals(new CodeType[]{Types.STRING, CodeAPI.getJavaType(CharSequence.class)}, universalInstance.types());
        Assert.assertArrayEquals(new EnumValue[]{new EnumValueImpl(TYPE_TYPE, "REGISTER", 0), new EnumValueImpl(TYPE_TYPE, "LOG", 1)}, universalInstance.entryTypes());
        Assert.assertEquals(0, universalInstance.flag());
        Assert.assertEquals(CodeAPI.getJavaType(Entry.class), universalInstance.annotationType());
    }

    @Test
    public void aptTest() throws Exception {

        CodeType TYPE_TYPE = CodeAPI.getJavaType(Type.class);

        JavaFileObject TEST = JavaFileObjects.forResource("Test.java");
        IntContainer intContainer = new IntContainer();

        JavaSourcesSubjectFactory.javaSources()
                .getSubject(new Fail(),
                        CollectionUtils.listOf(TEST))
                .processedWith(new AbstractProcessor() {
                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

                        Set<? extends Element> with = roundEnv.getElementsAnnotatedWith(Entry.class);

                        for (Element element : with) {

                            intContainer.add();

                            List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

                            AnnotationMirror annotationMirror = annotationMirrors.get(0);

                            UniversalEntry universalInstance = AnnotationsKt.getUniversalInstance(annotationMirror, UniversalEntry.class);

                            Assert.assertEquals("a", universalInstance.name());
                            Assert.assertArrayEquals(new int[]{0, 1, 2}, universalInstance.ids());
                            Assert.assertArrayEquals(new CodeType[]{new PlainCodeType("com.github.jonathanxd.codeapi.extra.test.Test"), CodeAPI.getJavaType(CharSequence.class)}, universalInstance.types());
                            Assert.assertArrayEquals(new EnumValue[]{new EnumValueImpl(TYPE_TYPE, "REGISTER", -1), new EnumValueImpl(TYPE_TYPE, "LOG", -1)}, universalInstance.entryTypes());
                            Assert.assertEquals(0, universalInstance.flag());
                            Assert.assertEquals(CodeAPI.getJavaType(Entry.class), universalInstance.annotationType());
                        }

                        return false;
                    }

                    @Override
                    public Set<String> getSupportedAnnotationTypes() {
                        return CollectionUtils.setOf("com.github.jonathanxd.codeapi.extra.test.Entry");
                    }

                    @Override
                    public SourceVersion getSupportedSourceVersion() {
                        return SourceVersion.RELEASE_8;
                    }
                })
                .compilesWithoutError();

        Assert.assertTrue(intContainer.get() > 0);
    }

    public interface UniversalEntry {

        CodeType[] types();

        String name();

        EnumValue[] entryTypes();

        int[] ids();

        int flag();

        CodeType annotationType();
    }

    public static class Fail extends FailureStrategy {
    }

}


