### Shutdown a file watcher by name

```java
watcherNew( "shutdownWatcher", getTempDirectory() );
watcherShutdown( "shutdownWatcher" );
writeOutput( "shutdown" );

```

Result: shutdown
