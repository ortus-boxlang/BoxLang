### Serialize a BoxLang object to binary

```java
data = { name : "test", values : [ 1, 2, 3 ] }
serialized = objectSerialize( data )
writeOutput( isBinary( serialized ) )
```

Result: true

### Serialize to a file as well

When a file path is given the binary data is saved there, and it is still returned.

```java
data = { name : "test" }
objectSerialize( data, getTempDirectory() & "data.ser" )
writeOutput( fileExists( getTempDirectory() & "data.ser" ) )
```

Result: true

### Serializing null throws

```java
try {
    objectSerialize( nullValue() )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: The object to serialize is null
