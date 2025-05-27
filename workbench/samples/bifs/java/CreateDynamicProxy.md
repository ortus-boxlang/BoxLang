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

Result: 

