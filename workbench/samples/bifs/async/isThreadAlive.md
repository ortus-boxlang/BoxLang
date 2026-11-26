### Check if a thread is alive

```java
threadNew( () => sleep( 300 ), {}, "worker" )
writeOutput( isThreadAlive( "worker" ) )
threadJoin( "worker" )
writeOutput( "," & isThreadAlive( "worker" ) )
```

Result: true,false

### Unknown thread

An exception is thrown if no thread has that name.

```java
try {
	isThreadAlive( "ghost" )
} catch ( any e ) {
	writeOutput( e.message.left( 37 ) )
}
```

Result: No thread with name [ghost] not found
