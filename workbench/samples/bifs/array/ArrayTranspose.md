### Swap rows and columns

Pass two or more arrays of the same length. The first elements of every array become the first row of the result, and so on.

```java
result = arrayTranspose( [ 1, 2, 3 ], [ 4, 5, 6 ] )
writeOutput( JSONSerialize( result ) )
```

Result: [[1,4],[2,5],[3,6]]

### Using the member function

The array the member function is called on is the first array to transpose.

```java
result = [ 1, 2 ].transpose( [ 3, 4 ], [ 5, 6 ] )
writeOutput( JSONSerialize( result ) )
```

Result: [[1,3,5],[2,4,6]]

### Arrays must have the same length

An exception is thrown when the arrays have different lengths.

```java
try {
	arrayTranspose( [ 1, 2, 3 ], [ 4, 5 ] )
} catch ( any e ) {
	writeOutput( e.message.left( 55 ) )
}
```

Result: All arrays passed in to arrayTranspose must be the same
