### Keep only elements that are also in another collection

Elements NOT present in the argument collection are removed.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
s.retainAll( [ 3, 4, 999 ] );
writeOutput( s.size() );

```

Result: 2

### Intersect a Set in-place

```java
a = setOf( "a", "b", "c", "d" );
a.retainAll( [ "b", "d" ] );
writeOutput( a.size() );

```

Result: 2
