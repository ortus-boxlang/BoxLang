### List the registered caches

The `default` cache is always registered.

```java
writeOutput( cacheNames().contains( "default" ) )
```

Result: true

### The result is an array of names

```java
names = cacheNames()
writeOutput( isArray( names ) & "," & ( names.len() >= 1 ) )
```

Result: true,true
