### Test that no elements satisfy a predicate

Returns `true` when the callback returns `false` for every element.

```java
numbers = [ 1, 2, 3, 4 ];
writeOutput( numbers.none( ( n ) => n > 100 ) );

```

Result: true

### Returns false when at least one element matches

```java
numbers = [ 1, 2, 3 ];
writeOutput( numbers.none( ( n ) => n > 2 ) );

```

Result: false

### Using the global function form

```java
writeOutput( arrayNone( [ "a", "b" ], ( s ) => len( s ) > 5 ) );

```

Result: true
