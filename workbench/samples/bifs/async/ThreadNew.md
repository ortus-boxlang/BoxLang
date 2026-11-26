### Create a new thread

The thread starts running immediately. Use `threadJoin()` to wait for it.

```java
threadNew( () => {
	sleep( 100 )
	request.message = "thread finished"
}, {}, "myThread" )
threadJoin( "myThread" )
writeOutput( request.message )
```

Result: thread finished

### Create a thread with named arguments

```java
t = threadNew( runnable = () => {
	request.value = 42
}, name = "namedThread" )
threadJoin( "namedThread" )
writeOutput( request.value )
```

Result: 42

### Create a virtual thread

Pass `true` as the last argument to run on a virtual thread. The arguments are the runnable, attributes, name, priority and virtual.

```java
threadNew( () => {
	request.isVirtual = threadCurrent().isVirtual()
}, {}, "virtualThread", "low", true )
threadJoin( "virtualThread" )
writeOutput( request.isVirtual )
```

Result: true

### The thread object is returned

The returned Java Thread can be used to monitor the thread.

```java
t = threadNew( () => sleep( 100 ), {}, "monitored" )
writeOutput( t.isAlive() )
threadJoin( "monitored" )
```

Result: true

### Invalid priority

The priority must be one of high, low or normal.

```java
try {
	threadNew( () -> 1, {}, "badPriority", "urgent" )
} catch ( any e ) {
	writeOutput( "invalid priority" )
}
```

Result: invalid priority
