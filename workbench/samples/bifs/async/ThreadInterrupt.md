### Interrupt a thread by ID

```java
future = asyncRun( () => {
    sleep( 5000 );
    return "done";
} );
threadId = future.getThread().getId();
threadInterrupt( threadId );
writeOutput( "interrupted" );

```

Result: interrupted
