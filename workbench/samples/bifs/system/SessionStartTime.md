### Get the session start time

```java
startTime = sessionStartTime()
writeOutput( isDate( startTime ) )
```

Result: true

### How long has the session been active

```java
minutes = dateDiff( "n", sessionStartTime(), now() )
writeOutput( minutes >= 0 )
```

Result: true
