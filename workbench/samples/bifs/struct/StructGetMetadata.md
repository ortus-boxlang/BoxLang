### Get struct metadata

```java
data = { name: "test", value: 42 };
meta = structGetMetadata( data );
writeOutput( meta.containsKey( "type" ) );

```

Result: true

### Using the $bx member

```java
data = { a: 1, b: 2 };
meta = data.$bx.meta;
writeOutput( meta.type );

```

Result: Struct
