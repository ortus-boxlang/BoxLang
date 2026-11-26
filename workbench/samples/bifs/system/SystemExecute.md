### Execute a system command

```java
result = systemExecute( "echo", "hello" )
writeOutput( result.output.contains( "hello" ) )

```

Result: true

### Execute with working directory

```java
result = systemExecute( name = "pwd", directory = getCanonicalPath( "." ) )
writeOutput( result.output.trim() == getCanonicalPath( "." ) )

```

Result: true

### Stream output as it arrives

```java
lines = []
result = systemExecute(
    name = "sh",
    arguments = [ "-c", "echo first; sleep 2; echo second" ],
    onLine = ( line, stream ) => lines.append( line )
)
writeOutput( lines.len() )

```

Result: 2

### Cancel a running process

```java
start = getTickCount()
result = systemExecute(
    name = "sleep",
    arguments = "30",
    isCancelled = () => getTickCount() - start > 500
)
writeOutput( result.cancelled )

```

Result: true

### Merge stderr into stdout

```java
result = systemExecute(
    name = "sh",
    arguments = [ "-c", "echo out; echo err >&2" ],
    mergeErrors = true
)
writeOutput( result.output.contains( "err" ) )

```

Result: true

### Inspect the result struct

The result contains output, error, timeout, terminated, pid, exitCode and cancelled.

```java
result = systemExecute( "echo", "hello" )
writeOutput( result.exitCode & " " & result.timeout )
```

Result: 0 false
