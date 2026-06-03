### Find the first element that satisfies a predicate

Returns the matching element, or `null` when nothing matches.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
found = s.find( ( Any v ) => v > 3 );
writeOutput( found >= 4 ? "yes" : "no" );

```

Result: yes

### Returns null when no element matches

```java
s = setOf( 1, 2, 3 );
found = s.find( ( Any v ) => v > 100 );
writeOutput( isNull( found ) ? "null" : found );

```

Result: null
