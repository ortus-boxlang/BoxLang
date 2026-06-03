### Flatten a nested array completely

When no depth is specified, all levels of nesting are flattened.

```java
nested = [ 1, [ 2, [ 3, [ 4 ] ] ] ];
result = nested.flatten();
writeOutput( result.toString() );

```

Result: [1, 2, 3, 4]

### Flatten to a specific depth

```java
nested = [ 1, [ 2, [ 3, [ 4 ] ] ] ];
result = nested.flatten( 1 );
writeOutput( result.toString() );

```

Result: [1, 2, [3, [4]]]

### Flatten two levels deep

```java
nested = [ 1, [ 2, [ 3, [ 4 ] ] ] ];
result = nested.flatten( 2 );
writeOutput( result.toString() );

```

Result: [1, 2, 3, [4]]

### Using the global function form

```java
result = arrayFlatten( [ [ "a", "b" ], [ "c" ] ] );
writeOutput( result.toString() );

```

Result: [a, b, c]
