### Get all registered file watchers

```java
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
watcherNew( "w2", getTempDirectory(), ( event ) -> {} )
all = watcherGetAll()
writeOutput( all.len() gte 2 )
```

Result: true

### Access a watcher from the returned struct

```java
watcherNew( "w1", getTempDirectory(), ( event ) -> {} )
all = watcherGetAll()
writeOutput( all.w1.getName() )
```

Result: w1

### Returns an empty struct when no watchers exist

```java
watcherShutdownAll()
writeOutput( watcherGetAll().len() )
```

Result: 0
