### Run a callback in the current context

The return value of the callback is returned.

```java
result = runThreadInContext( context = getBoxContext(), callback = () -> "executed in context" )
writeOutput( result )
```

Result: executed in context

### Run code inside a named application

The callback runs with the named application visible.

```java
result = runThreadInContext( applicationName = "myApp", callback = () => application.name ?: "unnamed" )
writeOutput( result )
```

Result: the application scope of myApp is available to the callback; the value depends on the application

### Context and application name are exclusive

```java
try {
    runThreadInContext( applicationName = "myApp", context = getBoxContext(), callback = () -> 1 )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Context and applicationName/sessionId are mutually exclusive.
