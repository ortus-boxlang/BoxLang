### Restart a file watcher

```java
watcherNew( "restartWatcher", getTempDirectory(), ( event ) -> {} )
watcherStart( "restartWatcher" )
w = watcherRestart( "restartWatcher" )
writeOutput( w.getStateAsString() )
```

Result: RUNNING

### Restarting an unknown watcher throws an error

```java
try {
    watcherRestart( "nonexistent" )
} catch( any e ) {
    writeOutput( e.message )
}
```

Result: No watcher named [nonexistent] is registered.
