### Get the current call stack

```java
stack = callStackGet()
writeOutput( isArray( stack ) )
```

Result: true

### Limit the number of frames

```java
function inner() {
    return callStackGet( maxFrames = 1 )
}
frames = inner()
writeOutput( isArray( frames ) && frames.len() <= 1 )
```

Result: true

### Inspect the stack from a recursive function

Each call adds frames to the stack, so the stack gets deeper as the recursion goes.

```java
function factorial( n ) {
    if ( n == 1 ) {
        return callStackGet().len()
    }
    return factorial( n - 1 )
}
writeOutput( factorial( 3 ) > factorial( 1 ) )
```

Result: true
