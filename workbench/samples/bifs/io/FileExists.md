### Checks if file at the given path exists.




```java
var myFile = "/path/to/the/file.jpg"
if( fileExists( expandPath( myFile ) ) ) {
	writeOutput( myFile & "exists!" )
}

```


### Additional Examples


```java
var filePath = "path/to/my/file.md"
if( fileExists( filePath ) ) echo
"it exists"

```

### Files, directories and missing paths

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/a.txt", "a" )
writeOutput( fileExists( dir & "/a.txt" ) & " " & fileExists( dir ) & " " & fileExists( dir & "/nope.txt" ) )
directoryDelete( dir, true )
```

Result: true false false

### Disallow absolute paths with allowRealPath

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/a.txt", "a" )
try {
	fileExists( dir & "/a.txt", false )
} catch ( any e ) {
	writeOutput( e.message.replace( dir, "<dir>" ) )
}
directoryDelete( dir, true )
```

Result: The file or path argument [<dir>/a.txt] is an absolute path. This is disallowed when the allowRealPath argument is set to false.
