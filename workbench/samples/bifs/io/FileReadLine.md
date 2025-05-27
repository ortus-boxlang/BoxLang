### Opens a file, reads a line then closes it.




```java
// Open File
var fileObject = fileOpen( "/path/to/file.txt" );
// Perform Actions
try {
	// Read Line
	writeOutput( fileReadLine( fileObject ) );
}
// Error Handling
 catch (any ex) {
	// Report Exception
	writeDump( ex );
}finally {
	// Always Close
	// Close File
	fileClose( fileObject );
}

```

Result: 

### Additional Examples


```java
// Example reads lines of file one at a time into an array
filePath = "/path/to/file.txt";
openFile = fileopen( filePath, "read" );
lines = [];
// IMPORTANT: must close file, use try/catch/finally to do so
try {
	while (!fileIsEoF( openFile )) {
		arrayAppend( lines, fileReadLine( openFile ) );
	}
} catch (any e) {
	rethrow;
}finally {
	fileClose( openFile );
}

```


