### Serialize a BoxLang object to binary

```java
data = { name: "test", values: [ 1, 2, 3 ] };
serialized = objectSerialize( data );
writeOutput( isBinary( serialized ) );

```

Result: true
