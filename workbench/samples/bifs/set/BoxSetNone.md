### Test that no element satisfies a predicate

Returns `true` when the callback returns `false` for every element.

```java
s = [ 1, 2, 3, 4 ].toSet();
writeOutput( s.none( ( Any v ) => v > 100 ) );

```

Result: true

### Returns false as soon as one element matches

```java
s = setOf( 1, 2, 3 );
writeOutput( s.none( ( Any v ) => v > 2 ) );

```

Result: false
