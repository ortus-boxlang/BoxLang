### List the registered cache providers

The result is an array of provider names. Providers are registered by modules, so it is empty when no cache modules are installed.

```java
providers = cacheProviders()
writeOutput( isArray( providers ) )
```

Result: true

### Providers in a core only runtime

With no cache provider modules installed there are no registered providers.

```java
writeOutput( cacheProviders().len() )
```

Result: 0
