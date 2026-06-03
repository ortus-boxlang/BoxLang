### Return elements common to both Sets

```java
a = [ 1, 2, 3 ].toSet();
b = [ 3, 4, 5 ].toSet();
i = a.intersection( b );
writeOutput( i.size() );

```

Result: 1

### Using the * operator shorthand

```java
a = set{ 1, 2, 3 };
b = set{ 2, 3, 4 };
result = a * b;
writeOutput( result.size() );

```

Result: 2
