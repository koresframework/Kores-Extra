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

import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.EnumValue;
import com.github.jonathanxd.codeapi.extra.AnnotationsKt;
import com.github.jonathanxd.codeapi.extra.JavaAnnotationResolverFunc;
import com.github.jonathanxd.codeapi.extra.JsonAnnotationUnifierKt;
import com.github.jonathanxd.codeapi.extra.ModelAnnotationResolverFunc;
import com.github.jonathanxd.codeapi.extra.UnifiedAnnotation;
import com.github.jonathanxd.codeapi.extra.UnifiedAnnotationsUtilKt;
import com.github.jonathanxd.codeapi.type.CodeType;
import com.github.jonathanxd.codeapi.type.PlainCodeType;
import com.github.jonathanxd.codeapi.util.CodeTypeResolverFunc;
import com.github.jonathanxd.codeapi.util.CodeTypes;
import com.github.jonathanxd.codeapi.util.ImplicitCodeType;
import com.github.jonathanxd.iutils.collection.Collections3;
import com.github.jonathanxd.iutils.container.primitivecontainers.IntContainer;
import com.github.jonathanxd.iutils.function.checked.function.EFunction;
import com.github.jonathanxd.iutils.map.MapUtils;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import kotlin.Unit;

public class AnnotationUnificationlTest {

    private static final String json = "{\n" +
            "  \"names\": [\n" +
            "    {\"value\": \"a\"},\n" +
            "    {\"value\": \"b\"}\n" +
            "  ],\n" +
            "  \"name\": {\"value\": \"a\"},\n" +
            "  \"entryTypes\": [\"REGISTER\", \"LOG\"],\n" +
            "  \"ids\": [0,1,2],\n" +
            "  \"flag\": 0,\n" +
            "  \"types\": [\"java.lang.String\", \"java.lang.CharSequence\"]\n" +
            "}";

    @Entry(names = {@Name("a"), @Name("b")},
            name = @Name("a"),
            entryTypes = {Type.REGISTER, Type.LOG}, ids = {0, 1, 2}, flag = 0, types = {String.class, CharSequence.class})
    public static final String a = "0";

    private static Class<?> invoke(java.lang.reflect.Type codeType) {
        return ImplicitCodeType.is(codeType, Name.class) ? UnifiedName.class : null;
    }

    private void assertEq_(List<EnumValue> enumValues, List<EnumValue> to) {
        Assert.assertEquals(enumValues.size(), to.size());

        for (int i = 0; i < enumValues.size(); i++) {
            EnumValue enumValue = enumValues.get(i);
            EnumValue toValue = to.get(i);
            Assert.assertTrue(ImplicitCodeType.is(enumValue.getType(), toValue.getType()));

            Assert.assertEquals(enumValue.getEnumEntry(), toValue.getEnumEntry());
        }
    }

    private void assert_(UnifiedEntry unifiedEntry, CodeType type) {
        CodeType TYPE_TYPE = CodeTypes.getCodeType(Type.class);

        String[] names = unifiedEntry.names().stream().map(UnifiedName::value).toArray(String[]::new);

        Assert.assertArrayEquals(new String[]{"a", "b"}, names);
        Assert.assertEquals("a", unifiedEntry.name().value());
        Assert.assertEquals(Collections3.listOf(0, 1, 2), unifiedEntry.ids());
        Assert.assertEquals(Collections3.listOf(type, CodeTypes.getCodeType(CharSequence.class)), unifiedEntry.types());
        assertEq_(Collections3.listOf(
                new EnumValue(TYPE_TYPE, "REGISTER"),
                new EnumValue(TYPE_TYPE, "LOG")),
                unifiedEntry.entryTypes());
        Assert.assertEquals(0, unifiedEntry.flag());
        Assert.assertEquals(CodeTypes.getCodeType(Entry.class), unifiedEntry.annotationType());

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
                AnnotationUnificationlTest::invoke);

        assert_(unifiedEntry, Types.STRING);
    }

    @Test
    public void testJsonAnnotation() throws Exception {

        EFunction<String, CodeType> f = s -> CodeTypes.getCodeType(Class.forName(s));

        UnifiedEntry unifiedEntry = JsonAnnotationUnifierKt.unifyJson(json, Entry.class, UnifiedEntry.class,
                new JavaAnnotationResolverFunc(AnnotationUnificationlTest::invoke,
                        CodeTypeResolverFunc.Companion.fromJavaFunction(f),
                        Entry.class.getClassLoader()));

        assert_(unifiedEntry, Types.STRING);
    }

    @Test
    public void testCodeAPIAnnotation() throws Exception {
        CodeType TYPE_TYPE = CodeTypes.getCodeType(Type.class);

        com.github.jonathanxd.codeapi.base.Annotation nameAnnotation = com.github.jonathanxd.codeapi.base.Annotation.Builder.builder()
                .type(CodeTypes.getCodeType(Name.class))
                .values(MapUtils.mapOf(
                        "value", "a"
                ))
                .build();

        com.github.jonathanxd.codeapi.base.Annotation nameAnnotationA = com.github.jonathanxd.codeapi.base.Annotation.Builder.builder()
                .type(CodeTypes.getCodeType(Name.class))
                .values(MapUtils.mapOf(
                        "value", "a"
                ))
                .build();

        com.github.jonathanxd.codeapi.base.Annotation nameAnnotationB = com.github.jonathanxd.codeapi.base.Annotation.Builder.builder()
                .type(CodeTypes.getCodeType(Name.class))
                .values(MapUtils.mapOf(
                        "value", "b"
                ))
                .build();

        com.github.jonathanxd.codeapi.base.Annotation annotation = com.github.jonathanxd.codeapi.base.Annotation.Builder.builder()
                .type(Entry.class)
                .values(MapUtils.mapOf(
                        "names", Collections3.listOf(nameAnnotationA, nameAnnotationB),
                        "name", nameAnnotation,
                        "entryTypes", Collections3.listOf(new EnumValue(TYPE_TYPE, "REGISTER"), new EnumValue(TYPE_TYPE, "LOG")),
                        "ids", Collections3.listOf(0, 1, 2),
                        "flag", 0,
                        "types", Collections3.listOf(Types.STRING, CodeTypes.getCodeType(CharSequence.class))
                ))
                .build();

        UnifiedEntry unifiedEntry = AnnotationsKt.getUnificationInstance(annotation, UnifiedEntry.class,
                AnnotationUnificationlTest::invoke);

        assert_(unifiedEntry, Types.STRING);
    }

    @Test
    public void aptTest() throws Exception {

        JavaFileObject TEST = JavaFileObjects.forResource("Test.java");
        IntContainer intContainer = new IntContainer();

        JavaSourcesSubjectFactory.javaSources()
                .getSubject(new Fail(),
                        Collections3.listOf(TEST))
                .processedWith(new AbstractProcessor() {


                    @Override
                    public synchronized void init(ProcessingEnvironment processingEnv) {
                        super.init(processingEnv);
                    }

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

                        Set<? extends Element> with = roundEnv.getElementsAnnotatedWith(Entry.class);

                        for (Element element : with) {

                            intContainer.add();

                            List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

                            AnnotationMirror annotationMirror = annotationMirrors.get(0);

                            UnifiedEntry unifiedEntry = AnnotationsKt.getUnificationInstance(annotationMirror, UnifiedEntry.class,
                                    codeType -> ImplicitCodeType.is(codeType, Name.class) ? UnifiedName.class : null,
                                    this.processingEnv.getElementUtils());


                            String[] names = unifiedEntry.names().stream().map(UnifiedName::value).toArray(String[]::new);

                            Assert.assertArrayEquals(new String[]{"a", "b"}, names);

                            assert_(unifiedEntry, new PlainCodeType("com.github.jonathanxd.codeapi.extra.test.Test"));
                        }

                        UnifiedEntry unifiedEntry = JsonAnnotationUnifierKt.unifyJson(json, Entry.class, UnifiedEntry.class,
                                new ModelAnnotationResolverFunc(AnnotationUnificationlTest::invoke,
                                        this.processingEnv.getElementUtils()));

                        assert_(unifiedEntry, Types.STRING);

                        return false;
                    }

                    @Override
                    public Set<String> getSupportedAnnotationTypes() {
                        return Collections3.setOf("com.github.jonathanxd.codeapi.extra.test.Entry");
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

        List<CodeType> types();

        UnifiedName name();

        List<UnifiedName> names();

        List<EnumValue> entryTypes();

        List<Integer> ids();

        int flag();

        CodeType annotationType();
    }

    public static class Fail extends FailureStrategy {
    }

}


