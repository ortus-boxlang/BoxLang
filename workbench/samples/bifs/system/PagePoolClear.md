### Clear the compiled page pool

Clears the cache of compiled templates and classes so they are recompiled on next use. The BIF returns true.

```java
writeOutput( pagePoolClear() )
```

Result: true

### Clear the pool after deploying new code

```java
result = pagePoolClear()
if ( result ) {
    writeOutput( "page pool has been cleared" )
}
```

Result: page pool has been cleared
