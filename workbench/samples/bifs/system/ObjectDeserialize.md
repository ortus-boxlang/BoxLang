### Deserialize a BoxLang object from binary

```java
data = { name : "test", value : 42 }
serialized = objectSerialize( data )
restored = objectDeserialize( serialized )
writeOutput( restored.name )
```

Result: test

### Deserialize from a file

A string input is treated as a file path to read the binary data from.

```java
objectSerialize( { name : "saved" }, getTempDirectory() & "data.ser" )
restored = objectDeserialize( getTempDirectory() & "data.ser" )
writeOutput( restored.name )
```

Result: saved
