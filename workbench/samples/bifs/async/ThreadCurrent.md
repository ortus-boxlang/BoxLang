### Get the current thread

Returns the Java Thread object that is executing the code.

```java
t = threadCurrent()
writeOutput( t.isAlive() )
```

Result: true

### Check if the current thread is virtual

The main thread is a platform thread, but a thread created with the virtual flag is virtual.

```java
writeOutput( threadCurrent().isVirtual() )
```

Result: false

### Inspect a virtual thread from inside a thread

```java
threadNew( () => {
	request.virtual = threadCurrent().isVirtual()
}, {}, "virtualThread", "normal", true )
threadJoin( "virtualThread" )
writeOutput( request.virtual )
```

Result: true
