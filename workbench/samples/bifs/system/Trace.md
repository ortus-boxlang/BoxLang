### Output trace information

The trace is recorded and logged to trace.log; it is only displayed when the runtime is in debug mode.

```java
trace( category = "test", text = "Trace message" )
writeOutput( "traced" )
```

Result: traced

### Trace with extra information

```java
trace( text = "Order processed", category = "orders", type = "Information", extrainfo = { id : 42 } )
```

Result: a trace record with the struct as extra info is recorded; no output

### Trace and abort

With abort set to true the request ends after the trace.

```java
trace( text = "Stopping here", abort = true )
writeOutput( "never reached" )
```

Result: the request is aborted after the trace and never reached is not output
