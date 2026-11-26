### Opens a file, reads a line then closes it.




```java
// Open File
var fileObject = fileOpen( "/path/to/file.txt" )
// Perform Actions
try {
	// Read Line
	writeOutput( fileReadLine( fileObject ) )
}
// Error Handling
 catch (any ex) {
	// Report Exception
	writeDump( ex )
}finally {
	// Always Close
	// Close File
	fileClose( fileObject )
}

```


### Additional Examples


```java
myFile = fileOpen( "filepath/filename.ext" )
writeDump( myfile )

```



```java
// how to access the underlying resource provider info
f = "ram://demo.txt"
fileWrite( f, "demo" )
dump( f.getResource().getResourceProvider().getScheme() )
 // ram ( i.e. the resource provider type)

```

### Open a file for writing then read it back

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/greeting.txt"
writer = fileOpen( file, "write" )
fileWriteLine( writer, "Hello" )
fileClose( writer )
reader = fileOpen( file, "read" )
writeOutput( fileReadLine( reader ) )
fileClose( reader )
directoryDelete( dir, true )
```

Result: Hello

### Invalid modes throw an error

```java
dir = createTempDirectory( prefix="docs-" )
try {
	fileOpen( dir & "/greeting.txt", "bogus" )
} catch ( any e ) {
	writeOutput( e.message )
}
directoryDelete( dir, true )
```

Result: Invalid file mode: [bogus]. Valid modes are: read, readbinary, write, append
