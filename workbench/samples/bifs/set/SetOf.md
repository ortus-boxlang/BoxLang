### Create a Set from positional varargs

Duplicate values are automatically removed.

```java
s = setOf( 1, 2, 2, 3 )
writeOutput( s.size() )

```

Result: 3

### Numbers and strings are normalized

Numerically equal values are duplicates even when passed as different literals, and strings are compared case-insensitively. A number and a string are never equal.

```java
s = setOf( 1, 1.0, "a", "A", "1" )
writeOutput( s.size() )

```

Result: 3
