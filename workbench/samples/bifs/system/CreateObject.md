### Create a BX / Component Instance

createObject Component


```java
<bx:script>
	tellTimeClass = createObject( "component", "appResources.components.tellTime" );
	tellTimeClass.getLocalTime();
</bx:script>

```


### Create a SOAP WebService Instance

createObject WebService


```java
<bx:script>
	ws = createObject( "webservice", "http://www.xmethods.net/sd/2001/TemperatureService.wsdl" );
	xlatstring = ws.getTemp( zipcode="55987" );
	writeOutput( "The temperature at 55987 is " & xlatstring );
</bx:script>
      
```


### Create a java class with specified bundle and version

createObject filesystem


```java
POIFSFileSystem = createObject( "java", "org.apache.poi.poifs.filesystem.POIFSFileSystem", "apache.poi", "3.11.0" );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FDISUxKzbFVci5KTSxJ9U%2FKSk0uUcjMU8hKLEtU0lEoSyyyTUaS0lBQgsqAab3SkswcPY%2FE4gzfxAIlBU0FTWuuFJzGlmeWZADNzizR0CTC6JzEvHS94JKizLx0p9K0tNQioPl6EN0gawDE8j51" target="_blank">Run Example</a>

```java
dump( label="CreateObject in java", var=createObject( "java", "java.util.HashMap" ) );
dump( label="CreateObject with init()", var=createObject( "java", "java.lang.StringBuffer" ).init() );

```


```java
dump( var=createObject( "class", "com.my.bx.class" ), expand=false );
// but even "class" is optional for bx
dump( var=createObject( "com.my.bx.class" ), expand=false );
// the modern new Object() syntax is also dynamic
dump( var=new "com.my.bx.class"(), expand=false );
dump( var=new com.my.bx.class(), expand=false );
myClass = "com.my.bx.class";
dump( var=new "#myClass#"(), expand=false );

```


