### Get all registered file watchers

```java
watcherNew( "w1", getTempDirectory() );
watcherNew( "w2", getTempDirectory() );
all = watcherGetAll();
writeOutput( all.len() gte 2 );

```

Result: true
