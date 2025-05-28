### Simple usage syntax

Call fileIsEOF on a file object and save the result to a variable.


```java
fileObj = fileOpen(expandPath('./file.txt');
isEndOfFile = fileIsEOF(fileObj);
```


### Using fileIsEOF to loop over all lines of a text file

Simplified example of using fileIsEOF to determine when all lines have been read from a file. Error handling omitted for clarity.


```java
// Error handling omitted for clarity.
// open a file for reading
fileObj = fileOpen( expandPath( "./file.txt" ), "read" );
// read each line until we read the end of the file.
// fileIsEOF(fileObj) == false until we've read in the last line.
while (!fileIsEOF( fileObj )) {
	lineContent = fileReadLine( fileObj );
	// do something with content of each line

}
// end of file reached, close the file handle
fileClose( fileObj );

```


### Additional Examples


```java
filePath = "/path/to/file.txt";
openFile = fileopen( filePath, "read" );
lines = [];
// IMPORTANT: must close file, use try/catch/finally to do so
try {
	// fileIsEOF(openFile) == false until we've read in the last line.
	while (!fileIsEoF( openFile )) {
		arrayAppend( lines, fileReadLine( openFile ) );
	}
} catch (any e) {
	rethrow;
}finally {
	fileClose( openFile );
}

```


