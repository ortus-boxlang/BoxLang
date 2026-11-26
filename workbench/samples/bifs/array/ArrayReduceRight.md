### Simple arrayReduceRight Example

Demonstrate how the function works from right to left.

<a href="https://try.boxlang.io/?code=eJw9jk0KwjAQhdedUzyykAZyhQo9gdCtuIjNoFk0lCGxDcW7GxN08723%2BOZnyaOIzRhwBXXKKlN4r5wrnaIbBd5%2Bmv3mxC7NPPnHM%2FZY2gqDHmPIWIVfprbAe2zNu72VMg2N4YyDOuGYJFQfpyqXKCa9DZSCpk185EuKaypX%2Fi%2FoDwKVNZ4%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	"a",
	"b",
	"c",
	"d"
]
newArray = arrayReduceRight( myArray, ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx
}, "" )
writeOutput( newArray )

```

Result: d4c3b2a1

### Reduce from the right using the member function

The accumulator receives the last element first.

```java
letters = [ "a", "b", "c" ]
writeOutput( letters.reduceRight( ( acc, letter ) -> acc & letter, "" ) )
```

Result: cba

### Subtract from the right

```java
writeOutput( arrayReduceRight( [ 1, 2, 3 ], ( acc, n ) -> acc - n, 10 ) )
```

Result: 4
