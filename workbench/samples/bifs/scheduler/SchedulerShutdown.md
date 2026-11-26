### Shutdown a scheduler

```java
schedulerNew( "shutdownSched" )
schedulerShutdown( "shutdownSched" )
writeOutput( "shutdown" )

```

Result: shutdown

### Force shutdown with a timeout in seconds

```java
schedulerNew( "shutdownSched2" )
schedulerShutdown( name = "shutdownSched2", force = true, timeout = 5 )
writeOutput( schedulerList().contains( "shutdownSched2" ) )

```

Result: true

### Error: scheduler does not exist

```java
try {
    schedulerShutdown( "doesNotExist" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: Scheduler [doesNotExist] not registered. Valid schedulers are: [...]
