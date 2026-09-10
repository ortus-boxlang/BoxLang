### Keep only elements that satisfy a predicate

Returns a new Set containing elements for which the callback returns `true`.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
evens = s.filter( ( Any v ) => v % 2 == 0 );
writeOutput( evens.size() );

```

Result: 2

### Filter strings by length

```java
s = setOf( "cat", "elephant", "ox", "deer" );
long = s.filter( ( Any v ) => len( v ) > 3 );
writeOutput( long.size() );

```

Result: 2
