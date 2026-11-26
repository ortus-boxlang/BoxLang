### Execute code asynchronously

Runs the callback in a separate thread and returns a BoxFuture.

```java
future = asyncRun( () -> "done" )
writeOutput( future.get() )
```

Result: done

### Chain async operations with then()

Each `then()` receives the result of the previous step.

```java
result = asyncRun( () -> 10 )
	.then( ( v ) -> v * 2 )
	.then( ( v ) -> v + 5 )
	.get()
writeOutput( result )
```

Result: 25

### Using the runAsync() alias

`runAsync()` is an alias of `asyncRun()`.

```java
future = runAsync( () -> "hello from async" )
writeOutput( future.get() )
```

Result: hello from async

### Recover from an error with onError()

If the callback throws, `onError()` provides a replacement value.

```java
result = asyncRun( () -> 1 / 0 )
	.onError( ( e ) -> "recovered" )
	.get()
writeOutput( result )
```

Result: recovered

### Run on a named executor

Pass the name of a registered executor to control where the code runs.

```java
future = asyncRun( () -> "on io-tasks", "io-tasks" )
writeOutput( future.get() )
```

Result: on io-tasks

### Use a timeout when calling get()

`get()` accepts a timeout in milliseconds.

```java
future = asyncRun( () -> 5 ).then( ( v ) -> v + 2 )
writeOutput( future.get( 2000 ) )
```

Result: 7
