### Combine all elements from both Sets

```java
a = [ 1, 2, 3 ].toSet();
b = [ 3, 4, 5 ].toSet();
u = a.union( b );
writeOutput( u.size() );

```

Result: 5

### Using the + operator shorthand

```java
a = set{ 1, 2, 3 };
b = set{ 3, 4, 5 };
result = a + b;
writeOutput( result.size() );

```

Result: 5

### Union with an Array (right-hand operand is accepted as-is)

```java
s = setOf( 1, 2, 3 );
u = s.union( [ 3, 4, 5 ] );
writeOutput( u.size() );

```

Result: 5
