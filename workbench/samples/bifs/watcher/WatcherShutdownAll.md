### Shutdown all registered file watchers

```java
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
watcherNew( "w2", getTempDirectory(), ( event ) -> {} )
watcherShutdownAll()
writeOutput( watcherList().len() )
```

Result: 0

### Force an immediate shutdown

```java
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
watcherStart( "w1" )
watcherShutdownAll( force = true )
writeOutput( watcherExists( "w1" ) )
```

Result: false
