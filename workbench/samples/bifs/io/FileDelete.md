### Script Syntax

Obtaining files within an archive folder and then removing them if they are older than one hour.


```java
var existingFiles = directoryList( expandPath( "/archive/" ), false, "query" )
for( file in existingFiles ) {
	if( dateDiff( "h", file.DATELASTMODIFIED, now() ) > 1 ) {
		fileDelete( file.DIRECTORY & "\" & file.NAME )
	}
}

```

Result: All files within the archive directory older than one hour will be deleted.

### Additional Examples


```java
fileDelete( "my/path/to/file.md" )

```

### Delete a file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/temp.txt"
fileWrite( file, "temp" )
fileDelete( file )
writeOutput( fileExists( file ) )
directoryDelete( dir, true )
```

Result: false

### Deleting a file which does not exist throws an error

```java
dir = createTempDirectory( prefix="docs-" )
try {
	fileDelete( dir & "/missing.txt" )
} catch ( any e ) {
	writeOutput( e.message.replace( dir, "<dir>" ) )
}
directoryDelete( dir, true )
```

Result: The file [<dir>/missing.txt] could not be found or does not exist.
