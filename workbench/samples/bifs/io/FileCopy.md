### Copy file from here to there




```java
fileCopy( sourceFile, destinationFile )

```


### Additional Examples


```java
fileCopy( "path/to/my/file.md", "new/location/for/file.md" )

```

### Copy a file

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/source.txt", "contents" )
fileCopy( dir & "/source.txt", dir & "/copy.txt" )
writeOutput( fileRead( dir & "/copy.txt" ) & " " & fileExists( dir & "/source.txt" ) )
directoryDelete( dir, true )
```

Result: contents true

### Copy into a directory, creating it if needed

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/source.txt", "contents" )
directoryCreate( dir & "/backup" )
fileCopy( dir & "/source.txt", dir & "/backup" )
fileCopy( dir & "/source.txt", dir & "/archive/2024/source.txt" )
writeOutput( fileExists( dir & "/backup/source.txt" ) & " " & fileExists( dir & "/archive/2024/source.txt" ) )
directoryDelete( dir, true )
```

Result: true true

### Refuse to overwrite an existing file

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/source.txt", "new" )
fileWrite( dir & "/target.txt", "old" )
try {
	fileCopy( dir & "/source.txt", dir & "/target.txt", true, false )
} catch ( any e ) {
	writeOutput( e.message.replace( dir, "<dir>" ) )
}
directoryDelete( dir, true )
```

Result: The target path of [<dir>/target.txt] already exists and the overwrite argument is set to false
