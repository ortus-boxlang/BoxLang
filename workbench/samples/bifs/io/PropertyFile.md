### Read values from a properties file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/app.properties"
writer = fileOpen( file, "write" )
fileWriteLine( writer, "db.host=localhost" )
fileWriteLine( writer, "db.port=5432" )
fileClose( writer )
props = propertyFile( file )
writeOutput( props.get( "db.host" ) & ":" & props.get( "db.port" ) & " " & props.get( "db.user", "none" ) )
directoryDelete( dir, true )
```

Result: localhost:5432 none

### Change values and store the file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/app.properties"
props = propertyFile( file )
props.set( "db.host", "example.com" ).set( "db.user", "admin" )
props.store()
reloaded = propertyFile( file )
writeOutput( reloaded.get( "db.host" ) & " " & reloaded.size() & " " & reloaded.exists( "db.user" ) )
directoryDelete( dir, true )
```

Result: example.com 2 true

### Create a new properties file

```java
dir = createTempDirectory( prefix="docs-" )
props = propertyFile()
props.set( "app.name", "demo" )
props.store( dir & "/new.properties" )
writeOutput( propertyFile( dir & "/new.properties" ).get( "app.name" ) )
directoryDelete( dir, true )
```

Result: demo
