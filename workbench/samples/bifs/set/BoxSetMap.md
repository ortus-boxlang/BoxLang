### Transform each element and return a new Set

The callback receives each value and must return the replacement value.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
doubled = s.map( ( Any v ) => v * 2 );
writeOutput( doubled.size() );

```

Result: 5

### Map strings to uppercase

```java
s = setOf( "apple", "banana", "cherry" );
upper = s.map( ( Any v ) => uCase( v ) );
writeOutput( upper.contains( "APPLE" ) );

```

Result: true
