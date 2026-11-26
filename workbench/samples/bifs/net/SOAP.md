### Create a client from a WSDL and list its operations

`soap()` downloads and parses the WSDL (the client is cached per WSDL URL) and returns a fluent client.

```java
ws = soap( "https://example.com/service.wsdl" )
writeOutput( ws.getOperations().len() )

```

Result: The number of operations defined in the WSDL, which depends on the service.

### Configure the client and invoke an operation

Operation arguments are passed as an array (positional) or a struct (named).

```java
ws = soap( "https://example.com/service.wsdl" )
	.timeout( 60 )
	.withBasicAuth( "user", "pass" )
result = ws.invoke( "GetData", [ 42 ] )
writeOutput( result )

```

Result: The value returned by the `GetData` operation of the web service.
