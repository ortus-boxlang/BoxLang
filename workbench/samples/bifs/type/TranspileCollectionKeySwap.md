### Swap keys in a collection during transpilation

```java
data = { oldKey: "value" };
result = transpileCollectionKeySwap( data, { oldKey: "newKey" } );
writeOutput( result.containsKey( "newKey" ) );

```

Result: true
