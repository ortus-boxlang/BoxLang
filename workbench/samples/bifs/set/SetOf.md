### Create a Set from positional varargs

Duplicate values are automatically removed.

```java
s = setOf( 1, 2, 2, 3 );
writeOutput( s.size() );

```

Result: 3

### All values must be the same conceptual type after normalization

Numerically equal values are treated as duplicates even when passed as different literals.

```java
s = setOf( 1, 1.0, "1" );
writeOutput( s.size() );

```

Result: 1
