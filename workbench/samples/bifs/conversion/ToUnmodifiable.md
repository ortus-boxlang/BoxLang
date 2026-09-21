### Convert to an unmodifiable (immutable) copy

```java
data = { name: "test" };
frozen = toUnmodifiable( data );
writeOutput( isObject( frozen ) );

```

Result: true

### Mutating an unmodifiable struct throws an error

```java
data = [ 1, 2, 3 ];
frozen = data.toUnmodifiable();
try {
    frozen.append( 4 );
    writeOutput( "error" );
} catch ( any e ) {
    writeOutput( "caught" );
}

```

Result: caught
