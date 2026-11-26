### List all registered file watchers

```java
watcherNew( "listWatcher", getTempDirectory(), ( event ) -> {} )
list = watcherList()
writeOutput( isArray( list ) )
```

Result: true

### The list contains the watcher names

```java
watcherShutdownAll()
watcherNew( "alpha", getTempDirectory(), ( event ) -> {} )
writeOutput( watcherList().toList() )
```

Result: alpha

### Returns an empty array when no watchers exist

```java
watcherShutdownAll()
writeOutput( watcherList().len() )
```

Result: 0
