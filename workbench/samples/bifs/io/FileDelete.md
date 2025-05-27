### Script Syntax

Obtaining files within an archive folder and then removing them if they are older than one hour.


```java
var existingFiles = directoryList( expandPath( "/archive/" ), false, "query" );
for( file in existingFiles ) {
	if( dateDiff( "h", file.DATELASTMODIFIED, now() ) > 1 ) {
		fileDelete( file.DIRECTORY & "\" & file.NAME );
	}
}

```

Result: All files within the archive directory older than one hour will be deleted.

### Additional Examples


```java
fileDelete( "my/path/to/file.md" );

```


