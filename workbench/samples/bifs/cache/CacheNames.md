### Get all cache region names

```java
cachePut( "test", "value", 60, "default" );
names = cacheNames();
writeOutput( isArray( names ) );

```

Result: true
