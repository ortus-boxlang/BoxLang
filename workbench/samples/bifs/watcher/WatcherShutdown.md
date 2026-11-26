### Shutdown a file watcher by name

Shutting down stops the watcher and removes it from the registry.

```java
watcherNew( "shutdownWatcher", getTempDirectory(), ( event ) -> {} )
writeOutput( watcherShutdown( "shutdownWatcher" ) & " " & watcherExists( "shutdownWatcher" ) )
```

Result: true false

### Shutting down an unknown watcher returns false

```java
writeOutput( watcherShutdown( "nonexistent" ) )
```

Result: false
