### Terminate a thread

```java
future = asyncRun( () => {
    sleep( 10000 );
    return "never reached";
} );
threadTerminate( future.getThread() );
writeOutput( "terminated" );

```

Result: terminated
