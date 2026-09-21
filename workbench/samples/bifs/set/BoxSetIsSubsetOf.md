### Check that every element of this Set is contained in another

```java
a = setOf( 1, 2 );
b = setOf( 1, 2, 3 );
writeOutput( a.isSubsetOf( b ) );

```

Result: true

### Returns false when the Set has elements not in the other

```java
a = setOf( 1, 2, 4 );
b = setOf( 1, 2, 3 );
writeOutput( a.isSubsetOf( b ) );

```

Result: false

### A Set is a subset of itself

```java
a = setOf( 1, 2, 3 );
writeOutput( a.isSubsetOf( a ) );

```

Result: true
