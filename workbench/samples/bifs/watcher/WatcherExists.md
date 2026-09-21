### Check if a file watcher exists by name

```java
watcherNew( "myWatcher", getTempDirectory() );
writeOutput( watcherExists( "myWatcher" ) );

```

Result: true

### Returns false for non-existent watcher

```java
writeOutput( watcherExists( "nonexistent" ) );

```

Result: false
