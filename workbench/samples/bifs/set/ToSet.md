### Convert an array to a Set

Deduplicates the array elements and returns a BoxSet.

```java
s = [ 1, 2, 2, 3 ].toSet();
writeOutput( s.size() );

```

Result: 3

### Convert to a linked (insertion-ordered) Set

```java
s = [ "c", "a", "b", "a" ].toSet( "linked" );
writeOutput( s.toArray().toString() );

```

Result: [c, a, b]

### Convert to a sorted Set

```java
s = [ 9, 1, 5, 3 ].toSet( "sorted" );
writeOutput( s.toArray().toString() );

```

Result: [1, 3, 5, 9]
