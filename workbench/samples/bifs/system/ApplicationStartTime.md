### Get the application start time

```java
startTime = applicationStartTime()
writeOutput( isDate( startTime ) )
```

Result: true

### Calculate how long the application has been running

```java
startTime = applicationStartTime()
seconds = dateDiff( "s", startTime, now() )
writeOutput( seconds >= 0 )
```

Result: true
