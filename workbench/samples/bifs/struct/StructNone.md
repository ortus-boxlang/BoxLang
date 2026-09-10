### Test that no struct entries satisfy a predicate

Returns `true` when the callback returns `false` for every entry.

```java
data = { a: 1, b: 2, c: 3 };
writeOutput( data.none( ( k, v ) => v > 100 ) );

```

Result: true

### Returns false when at least one entry matches

```java
data = { x: 1, y: 50, z: 100 };
writeOutput( data.none( ( k, v ) => v > 10 ) );

```

Result: false
