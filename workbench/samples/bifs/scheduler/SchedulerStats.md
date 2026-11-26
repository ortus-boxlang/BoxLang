### Get statistics for a single scheduler

```java
schedulerNew( "statsSched" )
stats = schedulerStats( "statsSched" )
writeOutput( isStruct( stats ) )

```

Result: true

### Get statistics for all schedulers

```java
schedulerNew( "statsSched2" )
stats = schedulerStats()
writeOutput( stats.keyExists( "statsSched2" ) )

```

Result: true

### Error: scheduler does not exist

```java
try {
    schedulerStats( "doesNotExist" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: Scheduler [doesNotExist] not registered. Registered schedulers are: [...]
