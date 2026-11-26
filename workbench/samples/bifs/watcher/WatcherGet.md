### Get a file watcher by name

```java
watcherNew( "myWatcher", getTempDirectory(), ( event ) -> {} )
w = watcherGet( "myWatcher" )
writeOutput( w.getName() )
```

Result: myWatcher

### Inspect the state of the watcher

```java
watcherNew( "stateWatcher", getTempDirectory(), ( event ) -> {} )
writeOutput( watcherGet( "stateWatcher" ).getStateAsString() )
```

Result: CREATED

### Getting an unknown watcher throws an error

```java
try {
    watcherGet( "nonexistent" )
} catch( any e ) {
    writeOutput( e.message )
}
```

Result: No watcher named [nonexistent] is registered.
