### Return elements that do NOT satisfy a predicate

The complement of `filter`.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
odds = s.reject( ( Any v ) => v % 2 == 0 );
writeOutput( odds.size() );

```

Result: 3

### Reject short strings

```java
s = setOf( "cat", "elephant", "ox", "deer" );
long = s.reject( ( Any v ) => len( v ) <= 3 );
writeOutput( long.size() );

```

Result: 2
