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
myFile = fileOpen( "filepath/filename.ext" );
writeDump( myfile );

```



```java
// how to access the underlying resource provider info
f = "ram://demo.txt";
fileWrite( f, "demo" );
dump( f.getResource().getResourceProvider().getScheme() );
 // ram ( i.e. the resource provider type)

```


