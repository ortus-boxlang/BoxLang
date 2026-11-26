### Create a new file watcher

A watcher is registered in the CREATED state. A listener is required, here a lambda that ignores the event.

```java
w = watcherNew( "myWatcher", getTempDirectory(), ( event ) -> {} )
writeOutput( w.getName() & " " & w.getStateAsString() )
```

Result: myWatcher CREATED

### Watcher with event callback

The listener receives a struct with the keys `kind`, `path`, `relativePath`, `watchRoot` and `timestamp`.

```java
w = watcherNew( "callbackWatcher", getTempDirectory(), ( event ) => {
    println( event.kind & " on " & event.path )
} )
writeOutput( isObject( w ) )
```

Result: true

### Use a struct of event handlers

```java
w = watcherNew(
    name = "structWatcher",
    paths = [ getTempDirectory() ],
    listener = {
        onCreate : ( event ) -> {},
        onModify : ( event ) -> {},
        onDelete : ( event ) -> {}
    },
    recursive = false
)
writeOutput( w.getName() )
```

Result: structWatcher

### Duplicate names are rejected unless forced

```java
watcherNew( "dupWatcher", getTempDirectory(), ( event ) -> {} )
try {
    watcherNew( "dupWatcher", getTempDirectory(), ( event ) -> {} )
} catch( any e ) {
    writeOutput( e.message )
}
```

Result: A watcher named [dupWatcher] is already registered. Use force=true to replace it.
