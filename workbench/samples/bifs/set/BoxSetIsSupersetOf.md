### Check that this Set contains all elements of another

The inverse of `isSubsetOf`.

```java
a = setOf( 1, 2, 3 );
b = setOf( 1, 2 );
writeOutput( a.isSupersetOf( b ) );

```

Result: true

### Returns false when the other Set has extra elements

```java
a = setOf( 1, 2 );
b = setOf( 1, 2, 3 );
writeOutput( a.isSupersetOf( b ) );

```

Result: false
