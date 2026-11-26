### Create a new executor service

```java
exec = executorNew( "myPool", "fixed", 4 )
writeOutput( exec.name() & "," & exec.type().toString() )
```

Result: myPool,FIXED

### Available executor types

```java
executorNew( "cachedPool", "cached" )
executorNew( "singleThread", "single" )
executorNew( "virtualThreads", "virtual" )
writeOutput( executorHas( "cachedPool" ) & executorHas( "singleThread" ) & executorHas( "virtualThreads" ) )
```

Result: truetruetrue

### Cached thread pool (unbounded)

```java
exec = executorNew( "workPool", "cached" )
writeOutput( exec.type().toString() )
```

Result: CACHED

### Creating an existing executor returns the original

If an executor with that name is already registered, it is returned as is and the new type is ignored.

```java
executorNew( "dup", "fixed", 2 )
exec = executorNew( "dup", "cached" )
writeOutput( exec.type().toString() )
```

Result: FIXED

### Invalid executor type

An unknown type throws an exception.

```java
try {
	executorNew( "bad", "weird" )
} catch ( any e ) {
	writeOutput( e.message.left( 16 ) )
}
```

Result: No enum constant
