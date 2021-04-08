---
title: Opt -
---
//[Kores-Extra](../../../index.md)/[com.github.jonathanxd.kores.extra](../index.md)/[Opt](index.md)



# Opt  
 [jvm] @[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html), [AnnotationTarget.PROPERTY_GETTER](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-p-r-o-p-e-r-t-y_-g-e-t-t-e-r/index.html)])  
  
annotation class [Opt](index.md)(**value**: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)<out ([Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) -> [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)>)

Used to support [java.util.Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) properties in unification. The [value](value.md) property defines the evaluator that will returns whether to return [java.util.Optional.empty](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html#empty--).



Example:



Annotation:

annotation class Localization(val value: KClass<*> = Default::class.java)

Unification interface:

interface UnifiedLocalization {  
    @Alias("value")  
    @Opt  
    fun getLocalization(): Optional<Class<*>>  
}

For @Localization, UnifiedLocalization.getLocalization() returns Optional.empty.



For @Localization(String::class.java), UnifiedLocalization.getLocalization() returns Optional.of(String::class.java).

   


## Constructors  
  
| | |
|---|---|
| <a name="com.github.jonathanxd.kores.extra/Opt/Opt/#kotlin.reflect.KClass[kotlin.Function1[kotlin.Any,kotlin.Boolean]]/PointingToDeclaration/"></a>[Opt](-opt.md)| <a name="com.github.jonathanxd.kores.extra/Opt/Opt/#kotlin.reflect.KClass[kotlin.Function1[kotlin.Any,kotlin.Boolean]]/PointingToDeclaration/"></a> [jvm] fun [Opt](-opt.md)(value: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)<out ([Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) -> [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)> = DefaultTypeCheck::class)   <br>|


## Properties  
  
|  Name |  Summary | 
|---|---|
| <a name="com.github.jonathanxd.kores.extra/Opt/value/#/PointingToDeclaration/"></a>[value](value.md)| <a name="com.github.jonathanxd.kores.extra/Opt/value/#/PointingToDeclaration/"></a> [jvm] val [value](value.md): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-class/index.html)<out ([Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) -> [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)>Evaluator that defines whether to return [java.util.Optional.empty](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html#empty--) or not.   <br>|


## Extensions  
  
|  Name |  Summary | 
|---|---|
| <a name="com.github.jonathanxd.kores.extra//checker/com.github.jonathanxd.kores.extra.Opt#/PointingToDeclaration/"></a>[checker](../checker.md)| <a name="com.github.jonathanxd.kores.extra//checker/com.github.jonathanxd.kores.extra.Opt#/PointingToDeclaration/"></a>[jvm]  <br>Content  <br>val [Opt](index.md).[checker](../checker.md): ([Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) -> [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)  <br>More info  <br>Gets the checker  <br><br><br>|

