### Serialize a struct to JSON

```java
data = { name: "Luis", age: 42 };
result = jsonSerialize( data );
writeOutput( result.contains( "Luis" ) );

```

Result: true

### Serialize an array to JSON

```java
items = [ 1, 2, 3 ];
result = jsonSerialize( items );
writeOutput( result );

```

Result: [1,2,3]
