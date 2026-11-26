### Interrupt a thread by name

The thread polls `isThreadInterrupted()` and stops when it has been interrupted.

```java
threadNew( () => {
	while ( !isThreadInterrupted( "" ) ) {
	}
	request.stopped = true
}, {}, "worker" )
sleep( 100 )
threadInterrupt( "worker" )
threadJoin( "worker" )
writeOutput( request.stopped )
```

Result: true

### Interrupt all threads

When no name is passed, every thread of the request is interrupted.

```java
threadNew( () => {
	while ( !isThreadInterrupted( "" ) ) {
	}
	request.stopped = true
}, {}, "worker2" )
sleep( 100 )
threadInterrupt()
threadJoin()
writeOutput( request.stopped )
```

Result: true

### Unknown thread

An exception is thrown if no thread has that name.

```java
try {
	threadInterrupt( "ghost" )
} catch ( any e ) {
	writeOutput( e.message.left( 37 ) )
}
```

Result: No thread with name [ghost] not found
