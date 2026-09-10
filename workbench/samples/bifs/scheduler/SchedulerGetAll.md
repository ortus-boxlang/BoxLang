### Get all registered schedulers

```java
schedulerNew( "sched1", 2 );
schedulerNew( "sched2", 4 );
all = schedulerGetAll();
writeOutput( all.len() gte 2 );

```

Result: true
