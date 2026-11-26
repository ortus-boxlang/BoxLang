### Get a registered scheduler by name

```java
schedulerNew( "myScheduler" )
sched = schedulerGet( "myScheduler" )
writeOutput( sched.getSchedulerName() )

```

Result: myScheduler

### Error: scheduler does not exist

```java
try {
    schedulerGet( "doesNotExist" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: Scheduler not found: doesNotExist
