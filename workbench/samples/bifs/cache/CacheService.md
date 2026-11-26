### Access the cache service

```java
svc = cacheService()
writeOutput( svc.hasCache( "default" ) & "," & svc.hasCache( "doesNotExist" ) )
```

Result: true,false

### Get the default cache from the service

```java
writeOutput( cacheService().getDefaultCache().getName() )
```

Result: default

### List the registered caches from the service

```java
writeOutput( cacheService().getRegisteredCaches().contains( "default" ) )
```

Result: true
