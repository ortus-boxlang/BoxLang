### Deserialize a BoxLang object from binary

```java
data = { name: "test", value: 42 };
serialized = objectSerialize( data );
restored = objectDeserialize( serialized );
writeOutput( restored.name );

```

Result: test
