### Create a new scheduler

```java
sched = schedulerNew( "myScheduler" )
writeOutput( sched.getSchedulerName() )

```

Result: myScheduler

### Scheduler with a specific timezone

```java
sched = schedulerNew( "nyScheduler", "America/New_York" )
writeOutput( isObject( sched ) )

```

Result: true

### Replace an existing scheduler with force

```java
schedulerNew( "dupSched" )
sched = schedulerNew( name = "dupSched", force = true )
writeOutput( sched.getSchedulerName() )

```

Result: dupSched

### Error: duplicate name without force

```java
schedulerNew( "dupSched2" )
try {
    schedulerNew( "dupSched2" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: A scheduler with the name [dupSched2] already exists
