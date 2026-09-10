### Stop all registered file watchers

```java
watcherNew( "w1", getTempDirectory() );
watcherNew( "w2", getTempDirectory() );
watcherStopAll();
writeOutput( "all stopped" );

```

Result: all stopped
