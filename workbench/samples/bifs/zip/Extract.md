### Extract a zip-file

Extract a zip-file and save the data in the "output-directory".


```java
extract( "zip", "test.zip", "output-directory" )

```


### Extract a multiple zip-files via a directory

Extract all zip-files, which are stored in the "multiple-directory" and save the data in the "output-directory".


```java
extract( "zip", "multiple-directory", "output-directory" )

```


### Additional Examples


```java
extract( "zip", "D:\test.zip", "D:\zipresult" )

```

### Extract a TAR or TGZ archive

Use `tar` for a raw TAR archive and `tgz` for a gzip-compressed TAR archive.

```java
extract( format="tar", source="archive.tar", destination="output-directory" )
extract( format="tgz", source="archive.tgz", destination="output-directory" )
```

When `format` is omitted, it is detected from the source extension. An unrecognized extension requires an explicit `format`.


### Extract a zip and list the files

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( "zip", base & "src", base & "out.zip" )
extract( "zip", base & "out.zip", base & "ex" )
writeOutput( fileExists( base & "ex/src/a.txt" ) & " " & fileExists( base & "ex/src/sub/b.log" ) )
```

Result: true true

### Extract only specific entries

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( "zip", base & "src", base & "out.zip" )
extract( source = base & "out.zip", destination = base & "ex", entryPaths = "src/a.txt" )
writeOutput( fileExists( base & "ex/src/a.txt" ) & " " & fileExists( base & "ex/src/sub/b.log" ) )
```

Result: true false

### Extract with a filter function

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( "zip", base & "src", base & "out.zip" )
extract( "zip", base & "out.zip", base & "ex", false, true, ( name ) -> name.endsWith( ".txt" ) )
writeOutput( fileExists( base & "ex/src/a.txt" ) & " " & fileExists( base & "ex/src/sub/b.log" ) )
```

Result: true false

### Extract a gzip-compressed tar

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( format = "tgz", source = base & "src", destination = base & "out.tgz" )
extract( source = base & "out.tgz", destination = base & "ex" )
writeOutput( fileExists( base & "ex/src/a.txt" ) )
```

Result: true

### A missing archive throws an error

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
try {
	extract( "zip", base & "missing.zip", base & "ex" )
} catch( any e ) {
	writeOutput( e.message contains "does not exist" )
}
```

Result: true
