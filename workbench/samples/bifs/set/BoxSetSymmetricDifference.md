### Elements in either Set but not in both

Equivalent to `(A ∪ B) − (A ∩ B)`.

```java
a = [ 1, 2, 3 ].toSet();
b = [ 3, 4, 5 ].toSet();
x = a.symmetricDifference( b );
writeOutput( x.size() );

```

Result: 4

### Using the ^ operator shorthand

```java
a = set{ 1, 2, 3 };
b = set{ 2, 3, 4 };
result = a ^ b;
writeOutput( result.size() );

```

Result: 2
