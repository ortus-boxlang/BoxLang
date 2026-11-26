### Compress a file

Compress the file "example.txt" to a zip-file.


```java
compress( "zip", "example.txt", "output.zip" )

```


### Compress a directory

Compress the "example-directory" to a zip-file.


```java
compress( "zip", "example-directory", "output.zip" )

```


### Additional Examples

### Compress a TAR or TGZ archive

Use `tar` for a raw TAR archive and `tgz` for a gzip-compressed TAR archive.

```java
compress( format="tar", source="example-directory", destination="output.tar" )
compress( format="tgz", source="example-directory", destination="output.tgz" )
```

When `format` is omitted, it is detected from the destination extension. An unrecognized extension requires an explicit `format`.

### Compress a folder into a zip and verify it

The returned value is the absolute path of the archive. The base folder is included as the root of the archive by default.

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
zipPath = compress( "zip", base & "src", base & "out.zip" )
writeOutput( getFileFromPath( zipPath ) & " " & isZipFile( zipPath ) )
```

Result: out.zip true

### Leave out the base folder

With the default includeBaseFolder=true the file would be extracted as ex/src/a.txt instead.

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( format = "zip", source = base & "src", destination = base & "out.zip", includeBaseFolder = false )
extract( "zip", base & "out.zip", base & "ex" )
writeOutput( fileExists( base & "ex/a.txt" ) )
```

Result: true

### Compress only the files matching a filter function

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress(
	format = "zip",
	source = base & "src",
	destination = base & "logs.zip",
	filter = ( name ) -> name.endsWith( ".log" )
)
extract( "zip", base & "logs.zip", base & "ex" )
writeOutput( fileExists( base & "ex/src/sub/b.log" ) & " " & fileExists( base & "ex/src/a.txt" ) )
```

Result: true false

### The format is detected from the extension

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
tgz = compress( source = base & "src", destination = base & "out.tgz" )
writeOutput( getFileFromPath( tgz ) )
```

Result: out.tgz

### An existing destination is not overwritten by default

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( "zip", base & "src", base & "out.zip" )
try {
	compress( "zip", base & "src", base & "out.zip" )
} catch( any e ) {
	writeOutput( "failed" )
}
compress( format = "zip", source = base & "src", destination = base & "out.zip", overwrite = true )
writeOutput( " replaced" )
```

Result: failed replaced
