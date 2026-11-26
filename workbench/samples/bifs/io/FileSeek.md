### Skip characters before reading

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/digits.txt"
fileWrite( file, "0123456789" )
fileObject = fileOpen( file, "read", "utf-8", true )
fileSeek( fileObject, 4 )
writeOutput( fileRead( fileObject ) )
fileClose( fileObject )
directoryDelete( dir, true )
```

Result: 456789

### Using the member function

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/digits.txt"
fileWrite( file, "0123456789" )
fileObject = fileOpen( file, "read", "utf-8", true )
fileObject.seek( 2 )
writeOutput( fileObject.readLine() )
fileObject.close()
directoryDelete( dir, true )
```

Result: 23456789

### A path is not accepted, only an open file object

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/digits.txt"
fileWrite( file, "0123456789" )
try {
	fileSeek( file, 4 )
} catch ( any e ) {
	writeOutput( e.message )
}
directoryDelete( dir, true )
```

Result: fileSeek() requires an existing file object to be passed that was created with fileOpen().
