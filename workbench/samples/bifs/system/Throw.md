### Throw a custom exception

Use throw to raise a custom application exception and catch it by its type.

```java
try {
    throw( type = "MyCustomError", message = "A custom error has been thrown!" )
} catch ( any e ) {
    writeOutput( e.type & ": " & e.message )
}
```

Result: MyCustomError: A custom error has been thrown!

### The default type is Custom

```java
try {
    throw( "boom" )
} catch ( any e ) {
    writeOutput( e.type & ": " & e.message )
}
```

Result: Custom: boom

### Throw with details and an error code

```java
try {
    throw( message = "thrown", detail = "deets", errorCode = "403", type = "Test" )
} catch ( any e ) {
    writeOutput( e.detail & " " & e.errorCode )
}
```

Result: deets 403

### Rethrow an existing exception

When only an exception object is passed, it is thrown as-is.

```java
try {
    try {
        throw( "inner" )
    } catch ( any e ) {
        throw( e )
    }
} catch ( any e2 ) {
    writeOutput( e2.message )
}
```

Result: inner
