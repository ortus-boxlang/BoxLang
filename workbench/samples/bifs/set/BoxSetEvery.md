### Test that every element satisfies a predicate

Returns `true` when the callback returns `true` for all elements. Returns `false` on the first failure.

```java
s = [ 1, 2, 3, 4 ].toSet();
allPositive = s.every( ( Any v ) => v > 0 );
allEven     = s.every( ( Any v ) => v % 2 == 0 );
writeOutput( allPositive & "," & allEven );

```

Result: true,false
