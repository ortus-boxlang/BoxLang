### Terminate a running thread

The thread is interrupted and, if it does not stop, forcibly stopped.

```java
threadNew( () => sleep( 10000 ), {}, "doomed" )
threadTerminate( "doomed" )
writeOutput( isThreadAlive( "doomed" ) )
```

Result: false

### Unknown thread

An exception is thrown if no thread has that name.

```java
try {
	threadTerminate( "ghost" )
} catch ( any e ) {
	writeOutput( e.message.left( 37 ) )
}
```

Result: No thread with name [ghost] not found
