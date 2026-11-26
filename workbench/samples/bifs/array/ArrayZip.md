### Combine two arrays element-by-element

Each result element is an array of the paired values.

```java
names = [ "Alice", "Bob" ]
ages = [ 30, 25 ]
result = names.zip( ages )
writeOutput( result[ 1 ].toString() )

```

Result: [Alice, 30]

### Arrays must have the same length

```java
letters = [ "a", "b", "c" ]
numbers = [ 1, 2 ]
try {
	letters.zip( numbers )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: The two arrays do not have the same length.  array1 length: [3]. array2 length: [2]

### Using the global function form

```java
result = arrayZip( [ 1, 2 ], [ "x", "y" ] )
writeOutput( result.toString() )

```

Result: [[1, x], [2, y]]

### Combine the pairs with a callback

The callback receives the paired values and its return value is used for that position.

```java
result = arrayZip( [ 1, 2 ], [ 10, 20 ], ( a, b ) -> a + b )
writeOutput( result.toList() )
```

Result: 11,22
