### Create and use a Java object

With the java type the class is loaded and the constructor is called with init().

```java
list = createObject( "java", "java.util.ArrayList" ).init()
list.add( "a" )
writeOutput( list.size() )
```

Result: 1

### Create a BoxLang class instance

For the class (or component) type the call returns a class reference; call init() to run the constructor.

```java
tellTime = createObject( "component", "appResources.components.tellTime" ).init()
writeOutput( tellTime.getLocalTime() )
```

Result: the local time returned by the class (depends on the class and the current time)

### Create a SOAP web service client

The className is the WSDL URL and the call returns a ready-to-use SOAP client.

```java
ws = createObject( "webservice", "http://www.xmethods.net/sd/2001/TemperatureService.wsdl" )
temp = ws.getTemp( zipcode = "55987" )
writeOutput( "The temperature at 55987 is " & temp )
```

Result: the temperature returned by the web service (requires network access to the WSDL)

### Unsupported types throw an error

Unknown types are passed to the onCreateObjectRequest event; with no listeners an exception is thrown.

```java
try {
    createObject( "bogus", "some.Name" )
} catch ( any e ) {
    writeOutput( "failed" )
}
```

Result: failed
