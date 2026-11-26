### Get all registered schedulers

```java
schedulerNew( "sched1" )
schedulerNew( "sched2" )
all = schedulerGetAll()
writeOutput( all.keyExists( "sched1" ) & " " & all.keyExists( "sched2" ) )

```

Result: true true

### Loop over the schedulers

```java
schedulerNew( "loopSched" )
names = []
schedulerGetAll().each( ( name, scheduler ) => names.append( name ) )
writeOutput( names.contains( "loopSched" ) )

```

Result: true
