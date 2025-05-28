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


### Additional Examples


```java
testFile = fileopen("filepath"),"write");
filewriteline(testfile,"I am the example of fileclose");
fileclose(testfile);
```


