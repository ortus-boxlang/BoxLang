### Restart a scheduler

```java
schedulerNew( "restartSched" )
schedulerRestart( "restartSched" )
writeOutput( "restarted" )

```

Result: restarted

### Force a restart with a timeout in seconds

```java
schedulerNew( "restartSched2" )
schedulerRestart( name = "restartSched2", force = true, timeout = 10 )
writeOutput( schedulerList().contains( "restartSched2" ) )

```

Result: true

### Error: scheduler does not exist

```java
try {
    schedulerRestart( "doesNotExist" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: Scheduler [doesNotExist] not registered. Valid schedulers are: [...]
