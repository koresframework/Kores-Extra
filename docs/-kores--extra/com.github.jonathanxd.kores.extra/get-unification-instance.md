---
title: getUnificationInstance -
---
//[Kores-Extra](../../index.md)/[com.github.jonathanxd.kores.extra](index.md)/[getUnificationInstance](get-unification-instance.md)



# getUnificationInstance  
[jvm]  
Content  
@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)()  
  
fun <[T](get-unification-instance.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)> [getUnificationInstance](get-unification-instance.md)(annotation: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), unificationInterface: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)<[T](get-unification-instance.md)>, additionalUnificationGetter: ([Type](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Type.html)) -> [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)<*>? = { null }, elements: [Elements](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/util/Elements.html)? = null): [T](get-unification-instance.md)  
More info  


Annotation systems unification.



This function unifies Java Reflection Annotations, Java model annotations and Kores Annotations via proxies.



You need to have a interface that defines unification of annotation properties, example:



Given:

public @interface Entry {  
  
    Class<?> type();  
    String name();  
  
}

You need to write a interface like this:

public interface EntryUnification {  
  
    KoresType type();  
    String name();  
  
}

You only need to use unification version of annotation property type in the unification interface.



Each annotation property type has it own unified version:

<ul><li>[Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html) -></li><li>[Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html) -></li><li>An [kotlin.Annotation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-annotation/index.html) -></li></ul>

For arrays only change the component type, example:

public @interface Entry {  
    Class<?>[] types();  
    String name();  
}  
  
  
public interface EntryUnification {  
  
    KoresType[] types();  
    String name();  
  
}

Obs: you can also add a annotationType method in the unification interface, this method returns the type of annotation (like [java.lang.annotation.Annotation.annotationType](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/Annotation.html#annotationType--)).

public interface EntryUnification {  
  
    KoresType[] types();  
    String name();  
  
    // Annotation type  
    KoresType annotationType();  
  
}

You can also provide additional unification interfaces. Example:

public @interface Id {  
  String value();  
}  
  
public @interface Entry {  
  Id id();  
  Class<?> type();  
}  
  
public interface IdUnification {  
  
  String value();  
  
  // Annotation type  
  KoresType annotationType();  
  
}  
  
public interface EntryUnification {  
  
  KoresType type();  
  IdUnification id();  
  
  // Annotation type  
  KoresType annotationType();  
  
}

**Obs:** Since 1.2, Unification of [Javax Annotation Mirror](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/AnnotationMirror.html) requires a non-null [elements](get-unification-instance.md) instance, if the [annotation](get-unification-instance.md) is an [AnnotationMirror](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/element/AnnotationMirror.html), then [elements](get-unification-instance.md)**must not** be null, otherwise you can pass null.

  



