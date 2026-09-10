### Return elements present in this Set but not in another

```java
a = [ 1, 2, 3 ].toSet();
b = [ 3, 4, 5 ].toSet();
d = a.difference( b );
writeOutput( d.size() );

```

Result: 2

### Using the - operator shorthand

```java
a = set{ 1, 2, 3 };
b = set{ 2, 3, 4 };
result = a - b;
writeOutput( result.size() );

```

Result: 1
