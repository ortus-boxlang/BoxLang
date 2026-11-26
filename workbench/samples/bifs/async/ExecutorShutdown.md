### Shutdown a registered executor

Shutting down an executor also removes it from the registry.

```java
executorNew( "tempPool", "fixed", 2 )
executorShutdown( "tempPool" )
writeOutput( executorHas( "tempPool" ) )
```

Result: false

### Shutdown with a timeout

Wait up to 5000 milliseconds for the running tasks to finish. Named arguments are used to skip the `force` argument.

```java
executorNew( "workPool", "fixed", 4 )
executorShutdown( name = "workPool", timeout = 5000 )
writeOutput( executorHas( "workPool" ) )
```

Result: false

### Force a shutdown

When `force` is true the running tasks are interrupted. The BIF always returns true, even for unknown executor names.

```java
executorNew( "busyPool", "fixed", 2 )
writeOutput( executorShutdown( "busyPool", true ) )
```

Result: true
