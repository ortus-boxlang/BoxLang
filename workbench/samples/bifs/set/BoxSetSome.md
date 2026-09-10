### Test that at least one element satisfies a predicate

Returns `true` on the first match.

```java
s = [ 1, 2, 3, 4 ].toSet();
writeOutput( s.some( ( Any v ) => v > 3 ) );

```

Result: true

### Returns false when nothing matches

```java
s = setOf( 1, 2, 3 );
writeOutput( s.some( ( Any v ) => v > 100 ) );

```

Result: false

### Using the any alias

`any` is an alias for `some`.

```java
s = setOf( "apple", "banana", "cherry" );
writeOutput( s.any( ( Any v ) => len( v ) > 5 ) );

```

Result: true
