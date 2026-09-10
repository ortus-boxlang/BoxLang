### Get the values of a struct as a Set

Duplicate values are merged, so the Set may be smaller than the struct.

```java
s = { a: 1, b: 1, c: 2 }.valueSet();
writeOutput( s.size() );

```

Result: 2

### Using the standalone function form

```java
s = structValueSet( { x: "foo", y: "bar", z: "foo" } );
writeOutput( s.size() );

```

Result: 2
