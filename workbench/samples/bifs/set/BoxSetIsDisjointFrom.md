### Check that two Sets share no elements

Returns `true` when the two Sets have an empty intersection.

```java
a = setOf( 1, 2 );
c = setOf( 9, 10 );
writeOutput( a.isDisjointFrom( c ) );

```

Result: true

### Returns false when Sets overlap

```java
a = setOf( 1, 2, 3 );
b = setOf( 3, 4, 5 );
writeOutput( a.isDisjointFrom( b ) );

```

Result: false
