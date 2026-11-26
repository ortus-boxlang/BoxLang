### Check if a file watcher exists by name

```java
watcherNew( "myWatcher", getTempDirectory(), ( event ) -> {} )
writeOutput( watcherExists( "myWatcher" ) )
```

Result: true

### Returns false for non-existent watcher

```java
writeOutput( watcherExists( "nonexistent" ) )
```

Result: false

### A shut down watcher no longer exists

```java
watcherNew( "tempWatcher", getTempDirectory(), ( event ) -> {} )
watcherShutdown( "tempWatcher" )
writeOutput( watcherExists( "tempWatcher" ) )
```

Result: false
