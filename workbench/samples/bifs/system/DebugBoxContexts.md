### Debug BoxLang context information

Returns one struct per context, from the outermost context to the current one.

```java
debug = debugBoxContexts()
writeOutput( isArray( debug ) )
```

Result: true

### Inspect the context struct keys

```java
debug = debugBoxContexts()
current = debug[ debug.len() ]
writeOutput( current.keyList() )
```

Result: name,templates,function,isInClass,declaringContext
