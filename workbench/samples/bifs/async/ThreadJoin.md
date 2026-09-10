### Wait for a thread to complete

```java
future = asyncRun( () => {
    sleep( 100 );
    return "finished";
} );
threadJoin( future.getThread() );
writeOutput( future.isDone() );

```

Result: true

### Join with a timeout

```java
future = asyncRun( () => {
    sleep( 50 );
    return "done";
} );
threadJoin( future.getThread(), 1000 );
writeOutput( future.get() );

```

Result: done
