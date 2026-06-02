### Shutdown all registered file watchers

```java
watcherNew( "w1", getTempDirectory() );
watcherNew( "w2", getTempDirectory() );
watcherShutdownAll();
writeOutput( "all shutdown" );

```

Result: all shutdown
