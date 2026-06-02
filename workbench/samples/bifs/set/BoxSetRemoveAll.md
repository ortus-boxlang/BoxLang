### Remove all elements found in another collection

Elements not present in the Set are silently ignored.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
s.removeAll( [ 1, 2 ] );
writeOutput( s.size() );

```

Result: 3

### Remove using another Set

```java
a = setOf( "a", "b", "c", "d" );
b = setOf( "b", "d" );
a.removeAll( b );
writeOutput( a.size() );

```

Result: 2
