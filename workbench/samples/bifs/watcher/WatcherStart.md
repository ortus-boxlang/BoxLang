### Start a file watcher

```java
watcherNew( "startWatcher", getTempDirectory(), ( event ) -> {} )
w = watcherStart( "startWatcher" )
writeOutput( w.getStateAsString() )
```

Result: RUNNING

### Starting a running watcher is harmless

```java
watcherNew( "startWatcher2", getTempDirectory(), ( event ) -> {} )
watcherStart( "startWatcher2" )
writeOutput( watcherStart( "startWatcher2" ).isRunning() )
```

Result: true

### Starting an unknown watcher throws an error

```java
try {
    watcherStart( "nonexistent" )
} catch( any e ) {
    writeOutput( e.message )
}
```

Result: No watcher named [nonexistent] is registered.
