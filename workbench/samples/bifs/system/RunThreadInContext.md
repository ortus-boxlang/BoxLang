### Run a thread in the current request context

```java
result = runThreadInContext( () => {
    return "executed in context";
} );
writeOutput( result );

```

Result: executed in context
