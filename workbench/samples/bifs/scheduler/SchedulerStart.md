### Start a scheduler from a class

Assumes a scheduler class exists at `models/ReportScheduler.bx`.

```java
sched = schedulerStart( "models.ReportScheduler" )
writeOutput( isObject( sched ) )

```

Result: true

### Override the scheduler name

```java
sched = schedulerStart( className = "models.ReportScheduler", name = "reports" )
writeOutput( sched.getSchedulerName() )

```

Result: reports
