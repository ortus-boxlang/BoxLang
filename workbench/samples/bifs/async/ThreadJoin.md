### Wait for a thread to complete

```java
threadNew( () => {
	sleep( 100 )
	request.done = true
}, {}, "worker" )
threadJoin( "worker" )
writeOutput( request.done )
```

Result: true

### Join several threads

Pass a comma-separated list of thread names.

```java
threadNew( () => {
	request.a = 1
}, {}, "joiner1" )
threadNew( () => {
	request.b = 2
}, {}, "joiner2" )
threadJoin( "joiner1,joiner2" )
writeOutput( request.a + request.b )
```

Result: 3

### Join all threads

With no thread name, all the threads of the request are joined.

```java
threadNew( () => {
	sleep( 50 )
	request.finished = true
}, {}, "anyThread" )
threadJoin()
writeOutput( request.finished )
```

Result: true

### Join with a timeout

The timeout is in milliseconds. When it elapses the join is abandoned and the thread keeps running.

```java
threadNew( () => sleep( 3000 ), {}, "longRunner" )
threadJoin( "longRunner", 50 )
writeOutput( isThreadAlive( "longRunner" ) )
threadTerminate( "longRunner" )
```

Result: true

### Unknown thread

An exception is thrown if no thread has that name.

```java
try {
	threadJoin( "ghost" )
} catch ( any e ) {
	writeOutput( e.message.left( 37 ) )
}
```

Result: No thread with name [ghost] not found
