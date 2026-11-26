### Create the One dimensional array

Uses the arrayNew function to create the new array

<a href="https://try.boxlang.io/?code=eJx1jsEKgkAQhs%2FuU%2Fx4UhDE8mpgh6AOerAXWGiKpW2VcZfFondvFaNTl28OM98%2FvyFfM8sJFeQ8G%2FIJCqRi7B%2B0bkSe48zSjIPSxPA93yX3zlxw7Rn74wFM1rGBnQYSSRKSbkU2c7Nwu7BEimqHl4iWRx3Z%2F4ci%2BiayI%2FFOE5i1Z4YglBniWmuoEZ60joPgWVlqnR1ciD11bdMRK6nVk35uKJB%2BAAnwS7g%3D" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1 )
someArray =
// Transpiler workaround for BIF return type
(( arg1, arg2, arg3, arg4 ) => {
	arraySet( arg1, arg2, arg3, arg4 )
	return true
})( newArray, 1, 4, "All is well" )
writeOutput( JSONSerialize( newArray ) )

```

Result: ["All is well","All is well","All is well","All is well"]

### Create the Two dimensional array

Uses the arrayNew function to create the new array

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKRgqaXHlQiWgFQ4VYMAFUouSWWVRcolCWmFOaqkSGEiO8SoyQTAlOTc7PS8GhxgibmvKizJJU%2F9KSgtISDQWvYH%2B%2F4NSizMSczKpUDQWYfgVNBU0Aqh1L6Q%3D%3D" target="_blank">Run Example</a>

```java
newArray = arrayNew( 2 )
newArray[ 1 ][ 1 ] = "First value"
newArray[ 1 ][ 1 ] = "First value"
newArray[ 1 ][ 2 ] = "First value"
newArray[ 2 ][ 1 ] = "Second value"
newArray[ 2 ][ 2 ] = "Second value"
writeOutput( JSONSerialize( newArray ) )

```

Result: [["First value","First value"],["Second value","Second value"]]

### Create unsynchronized array

Uses the arrayNew function to create the new unsynchronized array

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKhjoKaYk5xakKmlx5UHm9xIKC1LwUDQWl%2FLxUJaBEeVFmSap%2FaUlBaYmGglewv19walFmYk5mVaqGAkyTgqaCJgA7wCBM" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1, false )
newArray.append( "one" )
writeOutput( JSONSerialize( newArray ) )

```

Result: ["one"]

### Create an array using implicit notation

Instead of using arrayNew you can also create an array using square brackets.

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVYhW4OJUys9LVdIB0iXl%2BUpcsVzlRZklqf6lJQWlJRoKXsH%2BfsGpRZmJOZlVqRoKeTCtmgqaAAvHFjI%3D" target="_blank">Run Example</a>

```java
newArray = [ 
	"one",
	"two"
]
writeOutput( JSONSerialize( newArray ) )

```

Result: ["one","two"]

### Arrays are synchronized by default

```java
writeOutput( arrayNew().getMetadata().type & "," & arrayNew( 1, false ).getMetadata().type )
```

Result: synchronized,unsynchronized

### Create an empty array

```java
arr = arrayNew()
writeOutput( arr.len() )
```

Result: 0

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLVLBVSCwqSqz0Sy3XUDBU0OTS11fwzC3IyUzOLIHIKOTllySWZObncSXqJRYUpOalaChEx0JUlmeWZCiUJeaUphYjy3JxKiUq6QDJJBBpDMQmQBwdCySquTg5nf19A3xcIxSsFEqKSlO5OGuB4oWlqUUQRyhlpuikJJakKgGtAFmTUppboKGQqKAJAL%2FGLxg%3D" target="_blank">Run Example</a>

```java
a = arrayNew( 1 )
// Implicit array notation
a.append( [] )
// with values
a.append( [
	"a",
	"b",
	3,
	4,
	[],
	{
		COMPLEX : true
	},
	queryNew( "id,date" )
] )
dump( a )

```
