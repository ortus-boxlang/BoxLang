### Create a new file watcher

```java
w = watcherNew( "myWatcher", getTempDirectory() );
writeOutput( w.getName() );

```

Result: myWatcher

### Watcher with event callback

```java
w = watcherNew( "callbackWatcher", getTempDirectory(), ( event ) => {
    println( event.type() & " on " & event.path() );
} );
writeOutput( isObject( w ) );

```

Result: true
