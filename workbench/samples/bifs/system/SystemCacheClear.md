### Clear all caches

With no argument everything is cleared.

```java
systemCacheClear()
```

Result: all runtime caches are cleared; no output

### Clear the template cache

Clears the templates cached using the bx:cache component.

```java
systemCacheClear( "template" )
```

Result: only the template cache is cleared; no output

### An invalid cache name is rejected

```java
try {
    systemCacheClear( "bogus" )
} catch ( any e ) {
    writeOutput( "invalid cache name" )
}
```

Result: invalid cache name
