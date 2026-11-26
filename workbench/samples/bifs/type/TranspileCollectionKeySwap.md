### Swap keys in a collection during transpilation

```java
data = { oldKey: "value" }
result = transpileCollectionKeySwap( data, { oldKey: "newKey" } )
writeOutput( result.containsKey( "newKey" ) )

```

Result: true

### The original struct is not modified

```java
orig = { oldKey: 1 }
swapped = transpileCollectionKeySwap( orig, { oldKey: "newKey" } )
writeOutput( orig.keyList() & " " & swapped.keyList() )
```

Result: oldKey newKey

### Non-struct values are returned unchanged

```java
writeOutput( transpileCollectionKeySwap( "notastruct", { oldKey: "newKey" } ) )
```

Result: notastruct
