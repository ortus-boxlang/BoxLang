### Check whether a Set contains all elements of a collection

Returns `true` only when every element of the argument collection is present in the Set.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
writeOutput( s.containsAll( [ 2, 4 ] ) & "," & s.containsAll( [ 2, 9 ] ) );

```

Result: true,false
