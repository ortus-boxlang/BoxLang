### Tag Syntax




```java
<bx:set instance = new cfc.helloWorld() > 
 <bx:set dynInstnace = createDynamicProxy( instance, [
	"MyInterface"
	] ) > 
 <bx:set x = createObject( "java", "InvokeHelloProxy" ).init( dynInstnace ) > 
 <bx:set y = x.invokeHello() > 
 <bx:output>#y#</bx:output> 

```



### Pass a Box Class as a Java Runnable

Given a `Greeter.bx` class in the same folder with a `run()` method which writes its message:

```java
class {
	property name="message" default="hello"

	function init( message = "hello" ) {
		variables.message = arguments.message
		return this
	}

	function run() {
		writeOutput( "ran: " & variables.message )
	}
}
```

Create a proxy of an instance which implements `java.lang.Runnable`, so it can be handed to any Java API which expects one.

```java
greeter = new Greeter( "hi" )
runnable = createDynamicProxy( greeter, "java.lang.Runnable" )
runnable.run()
```

Result: ran: hi

### Pass a Box Class as a Java Comparator

The interfaces can be an array of names. Here the `Greeter` class has a `compare( a, b )` method which returns `a - b`, so the proxy can sort a Java list.

```java
comparator = createDynamicProxy( new Greeter(), [ "java.util.Comparator" ] )
list = createObject( "java", "java.util.ArrayList" ).init()
list.add( 3 )
list.add( 1 )
list.add( 2 )
createObject( "java", "java.util.Collections" ).sort( list, comparator )
writeOutput( list.toString() )
```

Result: [1, 2, 3]

### At least one interface is required

```java
try {
	createDynamicProxy( new Greeter(), [] )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: At least one interface must be passed to create a dynamic proxy
