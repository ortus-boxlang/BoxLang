### Filter cache entries by criteria

```java
cachePut( "key1", "value1", 60 );
cachePut( "key2", "value2", 60 );
filtered = cacheFilter( ( key, entry ) => key == "key1" );
writeOutput( filtered.len() );

```

Result: 1
