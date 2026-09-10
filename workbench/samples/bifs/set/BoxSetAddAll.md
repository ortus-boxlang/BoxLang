### Add all elements from another collection

Accepts an Array, another Set, or any iterable. Duplicates are silently ignored.

```java
s = [ 1, 2, 3 ].toSet();
s.addAll( [ 4, 5 ] );
writeOutput( s.size() );

```

Result: 5

### Merge two Sets

```java
a = setOf( "a", "b" );
b = setOf( "b", "c" );
a.addAll( b );
writeOutput( a.size() );

```

Result: 3
