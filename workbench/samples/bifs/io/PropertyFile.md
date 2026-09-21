### Read and parse a Java properties file

```java
// Create a temp properties file
propsFile = getTempDirectory() & "test.properties";
fileWrite( propsFile, "db.host=localhost`n`rdb.port=5432" );
props = propertyFile( propsFile );
writeOutput( props[ "db.host" ] );

```

Result: localhost
