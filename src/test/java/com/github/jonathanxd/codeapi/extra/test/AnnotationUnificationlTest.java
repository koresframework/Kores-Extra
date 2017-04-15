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
package com.github.jonathanxd.codeapi.extra.test;

import com.google.common.truth.FailureStrategy;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.EnumValue;
import com.github.jonathanxd.codeapi.base.impl.EnumValueImpl;
import com.github.jonathanxd.codeapi.builder.AnnotationBuilder;
import com.github.jonathanxd.codeapi.extra.AnnotationsKt;
import com.github.jonathanxd.codeapi.extra.UnifiedAnnotation;
import com.github.jonathanxd.codeapi.extra.UnifiedAnnotationsUtilKt;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.PlainCodeType;
import com.github.jonathanxd.iutils.collection.CollectionUtils;
import com.github.jonathanxd.iutils.container.primitivecontainers.IntContainer;
import com.github.jonathanxd.iutils.map.MapUtils;

import org.jetbrains.annotations.NotNull;
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

import kotlin.Unit;

public class AnnotationUnificationlTest {

    @Entry(name = @Name("a"), entryTypes = {Type.REGISTER, Type.LOG}, ids = {0, 1, 2}, flag = 0, types = {String.class, CharSequence.class})
    public static final String a = "0";

    private void assert_(UnifiedEntry unifiedEntry, CodeType type, int[] ordinals) {
        CodeType TYPE_TYPE = CodeAPI.getJavaType(Type.class);

        Assert.assertEquals("a", unifiedEntry.name().value());
        Assert.assertArrayEquals(new int[]{0, 1, 2}, unifiedEntry.ids());
        Assert.assertArrayEquals(new CodeType[]{type, CodeAPI.getJavaType(CharSequence.class)}, unifiedEntry.types());
        Assert.assertArrayEquals(new EnumValue[]{new EnumValueImpl(TYPE_TYPE, "REGISTER", ordinals[0]), new EnumValueImpl(TYPE_TYPE, "LOG", ordinals[1])}, unifiedEntry.entryTypes());
        Assert.assertEquals(0, unifiedEntry.flag());
        Assert.assertEquals(CodeAPI.getJavaType(Entry.class), unifiedEntry.annotationType());

        UnifiedEntry mapped = UnifiedAnnotationsUtilKt.map(unifiedEntry, stringMap -> {
            stringMap.put("name",
                    UnifiedAnnotationsUtilKt.map(stringMap.get("name"), stringObjectMap -> {
                        stringObjectMap.put("value", "y");

                        return Unit.INSTANCE;
                    }));

            return Unit.INSTANCE;
        });

        Assert.assertEquals("y", mapped.name().value());
    }

    @Test
    public void test() throws Exception {

        Annotation annotation = AnnotationUnificationlTest.class.getField("a").getDeclaredAnnotation(Entry.class);

        UnifiedEntry unifiedEntry = AnnotationsKt.getUnificationInstance(annotation, UnifiedEntry.class,
                codeType -> codeType.is(CodeAPI.getJavaType(Name.class)) ? UnifiedName.class : null);

        assert_(unifiedEntry, Types.STRING, new int[]{0, 1});
    }

    @Test
    public void testCodeAPIAnnotation() throws Exception {
        CodeType TYPE_TYPE = CodeAPI.getJavaType(Type.class);

        com.github.jonathanxd.codeapi.base.Annotation nameAnnotation = AnnotationBuilder.builder()
                .withType(CodeAPI.getJavaType(Name.class))
                .withValues(MapUtils.mapOf(
                        "value", "a"
                ))
                .build();

        com.github.jonathanxd.codeapi.base.Annotation annotation = AnnotationBuilder.builder()
                .withType(CodeAPI.getJavaType(Entry.class))
                .withValues(MapUtils.mapOf(
                        "name", nameAnnotation,
                        "entryTypes", new EnumValue[]{new EnumValueImpl(TYPE_TYPE, "REGISTER", 0), new EnumValueImpl(TYPE_TYPE, "LOG", 1)},
                        "ids", new int[] {0, 1, 2},
                        "flag", 0,
                        "types", new CodeType[]{Types.STRING, CodeAPI.getJavaType(CharSequence.class)}
                ))
                .build();

        UnifiedEntry unifiedEntry = AnnotationsKt.getUnificationInstance(annotation, UnifiedEntry.class,
                codeType -> codeType.is(CodeAPI.getJavaType(Name.class)) ? UnifiedName.class : null);

        assert_(unifiedEntry, Types.STRING, new int[]{0, 1});
    }

    @Test
    public void aptTest() throws Exception {

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

                            UnifiedEntry unifiedEntry = AnnotationsKt.getUnificationInstance(annotationMirror, UnifiedEntry.class,
                                    codeType -> codeType.is(CodeAPI.getJavaType(Name.class)) ? UnifiedName.class : null);

                            assert_(unifiedEntry, new PlainCodeType("com.github.jonathanxd.codeapi.extra.test.Test"), new int[]{-1, -1});
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

    public interface UnifiedName extends UnifiedAnnotation {

        String value();

        @NotNull
        @Override
        CodeType annotationType();

    }

    public interface UnifiedEntry {

        CodeType[] types();

        UnifiedName name();

        EnumValue[] entryTypes();

        int[] ids();

        int flag();

        CodeType annotationType();
    }

    public static class Fail extends FailureStrategy {
    }

}


