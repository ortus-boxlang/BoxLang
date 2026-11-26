### Standard function syntax

 Merge two arrays resulting in a single re-indexed array. All elements of both arrays are preserved.

<a href="https://try.boxlang.io/?code=eJxVjjEOwjAMRWdyiihTI%2FUKrEwgFjbEYMBNI5Xacpyi3p4khQF5eba%2Bnz1Ijmr39mrNzgHzhK4vdIe5VEMSmAM6czMLhhAx1XSZK71AqUUeIEK6IcncgBHSF5hRUhWMCJOO64HoWS1lC9YTSsDODvWP3v5OePOWqHjOylm7LXmhY0yl%2BbN46z%2FnUzym" target="_blank">Run Example</a>

```java
fruit = [ 
	"apple",
	"banana",
	"orange"
]
veggies = [
	"tomato",
	"carrot",
	"corn",
	"peas",
	"peppers"
]
healthyFoods = arrayMerge( fruit, veggies )
writeOutput( arrayToList( healthyFoods ) )

```

Result: apple,banana,orange,tomato,carrot,corn,peas,peppers

### Member function syntax

Merge two arrays resulting in a single re-indexed array. All elements of both arrays are preserved.

<a href="https://try.boxlang.io/?code=eJxVjTEOwjAMRWdyiihTIyFuwMqExMKGGAy4aaQ2thwH1NuTBBiQl2fr%2B%2F1RSlS7txdrNg6YZ3TbSjdIdTqSQArozNU8MYSIuaXrXWkBpR65gwjpB0lSB0bIX2BGyU0wIcw6rQeiR7OMrXu3oAQc7E%2FuzUui4qkoFx1sNcN6pmPMdfn799a%2FAYC4Ook%3D" target="_blank">Run Example</a>

```java
fruit = [ 
	"apple",
	"banana",
	"orange"
]
veggies = [
	"tomato",
	"carrot",
	"corn",
	"peas",
	"peppers"
]
healthyFoods = fruit.merge( veggies )
writeOutput( arrayToList( healthyFoods ) )

```

Result: apple,banana,orange,tomato,carrot,corn,peas,peppers

### Example where leaveIndex parameter is true

Merge two arrays resulting in a single re-indexed array. Where the both arrays have elements in the same position, only values from the first array are included in the result. Valid using standard or member function syntax.

Note how the first three elements of the veggies array are not merged because the fruit array already has values for elements 1-3.

<a href="https://try.boxlang.io/?code=eJxVjjEOwjAMRWdyCitTI%2FUKrEwgFjbEYMBNI5U6cpyi3p4k7YK8PFvfzx4kB4Uj3MEcLMY4ke0LPXEu1ZAFZ0%2FWPMxC3gdKNV3myh9UbpEXirBuyDI3iIRphxhJUhWMhJOO64n5XS1lC9cLiacOhvpHD%2FuJHlQygTNfCUrXrDFrt%2BVvfA6pNH8uB%2B4HV%2Fg%2Bsg%3D%3D" target="_blank">Run Example</a>

```java
fruit = [ 
	"apple",
	"banana",
	"orange"
]
veggies = [
	"tomato",
	"carrot",
	"corn",
	"peas",
	"peppers"
]
healthyFoods = arrayMerge( fruit, veggies, true )
writeOutput( arrayToList( healthyFoods ) )

```

Result: apple,banana,orange,peas,peppers

### Additional Examples


```java
aNames = [ 10412, 42, 33, 2, 999, 12769, 888 ]
aNames2 = [ 33, "b", "c", "d", "e", "f", "g" ]
dump( arrayMerge( aNames, aNames2 ) )
// member function
dump( aNames.merge( aNames2 ) )
dump( aNames.merge( aNames2, true ) )

```


