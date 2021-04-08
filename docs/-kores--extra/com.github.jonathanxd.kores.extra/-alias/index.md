---
title: Alias -
---
//[Kores-Extra](../../../index.md)/[com.github.jonathanxd.kores.extra](../index.md)/[Alias](index.md)



# Alias  
 [jvm] @[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html), [AnnotationTarget.PROPERTY_GETTER](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-p-r-o-p-e-r-t-y_-g-e-t-t-e-r/index.html)])  
  
annotation class [Alias](index.md)(**value**: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Defines the alias of the method. This alias is the name of the method in the annotations to unify. This allows more conventional names for interface methods rather than naming conventions used in annotations, so instead of naming a method in this way: age, you can name it as getAge and add [Alias](index.md) annotation with age value.



Example:



Annotation:

annotation class Person(val name: String, val age: Int)

Unification interface:

interface UnifiedPerson {  
    @Alias("name")  
    fun getName(): String  
    @Alias("age")  
    fun getAge(): Int  
}

We can also use Kotlin properties, but alias still required because Kotlin properties are compiled to getters methods (and setters if property is mutable, but this is irrelevant for unification).

   


## Constructors  
  
| | |
|---|---|
| <a name="com.github.jonathanxd.kores.extra/Alias/Alias/#kotlin.String/PointingToDeclaration/"></a>[Alias](-alias.md)| <a name="com.github.jonathanxd.kores.extra/Alias/Alias/#kotlin.String/PointingToDeclaration/"></a> [jvm] fun [Alias](-alias.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))   <br>|


## Properties  
  
|  Name |  Summary | 
|---|---|
| <a name="com.github.jonathanxd.kores.extra/Alias/value/#/PointingToDeclaration/"></a>[value](value.md)| <a name="com.github.jonathanxd.kores.extra/Alias/value/#/PointingToDeclaration/"></a> [jvm] val [value](value.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)   <br>|

