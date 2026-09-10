### List all registered file watchers

```java
watcherNew( "listWatcher", getTempDirectory() );
list = watcherList();
writeOutput( isArray( list ) );

```

Result: true
