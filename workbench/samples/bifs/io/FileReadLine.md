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
// Example reads lines of file one at a time into an array
filePath = "/path/to/file.txt"
openFile = fileopen( filePath, "read" )
lines = []
// IMPORTANT: must close file, use try/catch/finally to do so
try {
	while (!fileIsEoF( openFile )) {
		arrayAppend( lines, fileReadLine( openFile ) )
	}
} catch (any e) {
	rethrow
}finally {
	fileClose( openFile )
}

```

### Read a file line by line

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
writer = fileOpen( file, "write" )
fileWriteLine( writer, "alpha" )
fileWriteLine( writer, "beta" )
fileClose( writer )
reader = fileOpen( file )
writeOutput( fileReadLine( reader ) & "," & fileReadLine( reader ) )
fileClose( reader )
directoryDelete( dir, true )
```

Result: alpha,beta

### Using the member function

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
fileWrite( file, "alpha" )
reader = fileOpen( file )
writeOutput( reader.readLine() )
reader.close()
directoryDelete( dir, true )
```

Result: alpha
