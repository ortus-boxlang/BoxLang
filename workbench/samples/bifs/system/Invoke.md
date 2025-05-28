### Invoke a Java Method

Invokes the size method on a new HashMap object, which should return 0


```java
invoke( createObject( "java", "java.util.HashMap" ), "size" );

```

Result: 0

### Invoke a method on a component

Invokes the method named 'test' on the component Test.bx with one parameter


```java
obj = createObject( "component", "Test" );
invoke( obj, "test", {
	PARAMETER : "Test Data"
} );

```


### Invoke a method on a webservice with one argument

Invokes the method named 'test' on the webservice Test.bx with one argument


```java
obj = createObject( "webservice", "https://example.com/test.bx?wsdl" );
invoke( obj, "test", {
	ARGUMENT1 : "Test Data"
} );

```


### Invoke a method on a webservice with multiple arguments

Invokes the method named 'test' on the webservice Test.bx with multiple arguments


```java
obj = createObject( "webservice", "https://example.com/test.bx?wsdl" );
invoke( obj, "test", {
	ARGUMENT1 : "Test Data",
	ARGUMENT2 : "More Data",
	ARGUMENT3 : "Still More Data"
} );

```


### Additional Examples


```java
<bx:script>
	writeDump( label="structure with invoke()", var=invoke( variables, "myStruct", {
		A : "First"
	} ) );

	private function myStruct() {
		return "myStruct:" & JSONSerialize( arguments );
	}
	writeDump( label="Adding numbers with invoke()", var=invoke( variables, "calc", {
		A : 3,
		B : 2
	} ) );

	private function calc( numeric a, numeric b ) {
		return a + b;
	}
</bx:script>

```


