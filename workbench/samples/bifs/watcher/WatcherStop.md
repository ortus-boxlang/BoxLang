### Stop a file watcher by name

```java
watcherNew( "stopWatcher", getTempDirectory(), ( event ) -> {} )
watcherStart( "stopWatcher" )
w = watcherStop( "stopWatcher" )
writeOutput( w.getStateAsString() )
```

Result: STOPPED

### A stopped watcher stays registered

```java
watcherNew( "stopWatcher2", getTempDirectory(), ( event ) -> {} )
watcherStart( "stopWatcher2" )
watcherStop( "stopWatcher2" )
writeOutput( watcherExists( "stopWatcher2" ) )
```

Result: true
