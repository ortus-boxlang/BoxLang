### Simple usage syntax

Call fileIsEOF on a file object and save the result to a variable.


```java
fileObj = fileOpen(expandPath('./file.txt')
isEndOfFile = fileIsEOF(fileObj)
```


### Using fileIsEOF to loop over all lines of a text file

Simplified example of using fileIsEOF to determine when all lines have been read from a file. Error handling omitted for clarity.


```java
// Error handling omitted for clarity.
// open a file for reading
fileObj = fileOpen( expandPath( "./file.txt" ), "read" )
// read each line until we read the end of the file.
// fileIsEOF(fileObj) == false until we've read in the last line.
while (!fileIsEOF( fileObj )) {
	lineContent = fileReadLine( fileObj )
	// do something with content of each line

}
// end of file reached, close the file handle
fileClose( fileObj )

```


### Additional Examples


```java
filePath = "/path/to/file.txt"
openFile = fileopen( filePath, "read" )
lines = []
// IMPORTANT: must close file, use try/catch/finally to do so
try {
	// fileIsEOF(openFile) == false until we've read in the last line.
	while (!fileIsEoF( openFile )) {
		arrayAppend( lines, fileReadLine( openFile ) )
	}
} catch (any e) {
	rethrow
}finally {
	fileClose( openFile )
}

```

### Read every line of a file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
writer = fileOpen( file, "write" )
fileWriteLine( writer, "one" )
fileWriteLine( writer, "two" )
fileWriteLine( writer, "three" )
fileClose( writer )
reader = fileOpen( file, "read" )
lines = []
while ( !fileIsEOF( reader ) ) {
	lines.append( fileReadLine( reader ) )
}
fileClose( reader )
writeOutput( lines.len() & ": " & lines.toList() )
directoryDelete( dir, true )
```

Result: 3: one,two,three

### A path is not accepted, only an open file object

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
fileWrite( file, "one" )
try {
	fileIsEOF( file )
} catch ( any e ) {
	writeOutput( e.message )
}
directoryDelete( dir, true )
```

Result: fileIsEOF() requires an existing file object to be passed that was created with fileOpen().
