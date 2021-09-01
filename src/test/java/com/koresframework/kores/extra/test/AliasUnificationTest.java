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
package com.koresframework.kores.extra.test;

import com.github.jonathanxd.iutils.object.Default;
import com.github.jonathanxd.iutils.opt.OptObject;
import com.koresframework.kores.extra.Alias;
import com.koresframework.kores.extra.Opt;
import com.koresframework.kores.type.ImplicitKoresType;

import com.koresframework.kores.extra.AnnotationsKt;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

public class AliasUnificationTest {

    @Person(name = "Ajksa", age = 25)
    private final Object o = null;
    @Person(name = "Ajksa", age = 25, type = String.class)
    private final Object o2 = null;

    @Test
    public void test() throws NoSuchFieldException {
        Person o = AliasUnificationTest.class.getDeclaredField("o").getAnnotation(Person.class);

        UnifiedPerson unificationInstance = AnnotationsKt.getUnificationInstance(o, UnifiedPerson.class);

        Assert.assertEquals("Ajksa", unificationInstance.getName());
        Assert.assertEquals(25, unificationInstance.getAge());
        Assert.assertFalse(unificationInstance.getType().isPresent());
        Assert.assertFalse(unificationInstance.getType2().isPresent());
    }

    @Test
    public void testPresent() throws NoSuchFieldException {
        Person o = AliasUnificationTest.class.getDeclaredField("o2").getAnnotation(Person.class);

        UnifiedPerson unificationInstance = AnnotationsKt.getUnificationInstance(o, UnifiedPerson.class);

        Assert.assertEquals("Ajksa", unificationInstance.getName());
        Assert.assertEquals(25, unificationInstance.getAge());
        Assert.assertTrue(unificationInstance.getType().isPresent());
        Assert.assertTrue(unificationInstance.getType2().isPresent());
        Assert.assertEquals(ImplicitKoresType.getIdentification(String.class),
                ImplicitKoresType.getIdentification(unificationInstance.getType().get()));

        Assert.assertEquals(ImplicitKoresType.getIdentification(String.class),
                ImplicitKoresType.getIdentification(unificationInstance.getType2().getValue()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Person {
        String name();
        int age();
        Class<?> type() default Default.class;
    }

    public interface UnifiedPerson {
        @Alias("name")
        String getName();

        @Alias("age")
        int getAge();

        @Alias("type")
        @Opt
        Optional<Class<?>> getType();

        @Alias("type")
        @Opt
        OptObject<Class<?>> getType2();
    }

}
