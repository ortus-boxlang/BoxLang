### Check the current thread

Inside a thread, pass an empty name to check the current thread.

```java
threadNew( () => {
	request.result = isThreadInterrupted( "" )
}, {}, "checker" )
threadJoin( "checker" )
writeOutput( request.result )
```

Result: false

### Stop a loop when the thread is interrupted

A running thread can poll `isThreadInterrupted()` to know it should stop. The main code interrupts it by name.

```java
threadNew( () => {
	while ( !isThreadInterrupted( "" ) ) {
	}
	request.stopped = true
}, {}, "looper" )
sleep( 100 )
threadInterrupt( "looper" )
threadJoin( "looper" )
writeOutput( request.stopped )
```

Result: true
