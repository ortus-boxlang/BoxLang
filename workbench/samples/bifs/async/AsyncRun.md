### Execute code asynchronously

Runs the callback in a separate thread and returns a BoxFuture.

```java
future = asyncRun( () => sleep( 100 ) && return "done" );
writeOutput( future.get() );

```

Result: done

### Chain async operations with then()

```java
asyncRun( () => 10 )
    .then( ( v ) => v * 2 )
    .then( ( v ) => v + 5 )
    .thenAccept( ( v ) => writeOutput( v ) );

```

Result: 25

### Using the runAsync() alias

```java
future = runAsync( () => "hello from async" );
writeOutput( future.get() );

```

Result: hello from async
