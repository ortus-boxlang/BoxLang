### Create a new scheduler

```java
sched = schedulerNew( "myScheduler", 4 );
writeOutput( sched.getName() );

```

Result: myScheduler

### Scheduler with custom thread count

```java
sched = schedulerNew( "heavyTasks", 10 );
writeOutput( isObject( sched ) );

```

Result: true
