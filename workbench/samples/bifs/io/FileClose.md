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
testFile = fileopen("filepath"),"write")
filewriteline(testfile,"I am the example of fileclose")
fileclose(testfile)
```

### Write a line then close the file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/closed.txt"
fileObject = fileOpen( file, "write" )
fileWriteLine( fileObject, "first" )
fileClose( fileObject )
writeOutput( fileRead( file ).trim() )
directoryDelete( dir, true )
```

Result: first

### Always close the file in a finally block

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/data.txt"
fileWrite( file, "line one" )
fileObject = fileOpen( file )
try {
	writeOutput( fileReadLine( fileObject ) )
} finally {
	fileClose( fileObject )
}
directoryDelete( dir, true )
```

Result: line one
