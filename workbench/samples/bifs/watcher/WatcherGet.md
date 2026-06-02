### Get a file watcher by name

```java
watcherNew( "myWatcher", getTempDirectory() );
w = watcherGet( "myWatcher" );
writeOutput( w.getName() );

```

Result: myWatcher
