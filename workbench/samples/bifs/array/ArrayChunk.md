### Split an array into chunks of a given size

The final chunk may be shorter if the array does not divide evenly.

```java
numbers = [ 1, 2, 3, 4, 5 ];
chunks = numbers.chunk( 2 );
writeOutput( chunks.len() );

```

Result: 3

### Evenly divisible chunks

```java
numbers = [ 1, 2, 3, 4, 5, 6 ];
chunks = numbers.chunk( 3 );
writeOutput( chunks[ 1 ].toString() );

```

Result: [1, 2, 3]

### Using the global function form

```java
chunks = arrayChunk( [ "a", "b", "c", "d" ], 2 );
writeOutput( chunks.len() );

```

Result: 2
