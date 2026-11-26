### Stop all registered file watchers

```java
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
watcherNew( "w2", getTempDirectory(), ( event ) -> {} )
watcherStart( "w1" )
watcherStart( "w2" )
watcherStopAll()
writeOutput( watcherGet( "w1" ).getStateAsString() & " " & watcherGet( "w2" ).getStateAsString() )
```

Result: STOPPED STOPPED

### Stopped watchers remain registered

```java
watcherShutdownAll()
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
watcherStopAll( force = true )
writeOutput( watcherList().len() )
```

Result: 1
