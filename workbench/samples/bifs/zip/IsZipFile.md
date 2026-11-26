### Check if a file is a valid ZIP archive

```java
zipFile = getTempDirectory() & "test.zip"
// Create a simple zip for testing
fileWrite( getTempDirectory() & "hello.txt", "hello" )
compress( format = "zip", source = getTempDirectory() & "hello.txt", destination = zipFile, overwrite = true )
writeOutput( isZipFile( zipFile ) )

```

Result: true

### A zip file returns true and a text file returns false

```java
base = getTempDirectory() & "zipDemo/"
if( directoryExists( base ) ) directoryDelete( base, true )
directoryCreate( base & "src/sub", true )
fileWrite( base & "src/a.txt", "hello" )
fileWrite( base & "src/sub/b.log", "world" )
compress( "zip", base & "src", base & "out.zip" )
writeOutput( isZipFile( base & "out.zip" ) & " " & isZipFile( base & "src/a.txt" ) )
```

Result: true false

### A missing file returns false

```java
writeOutput( isZipFile( getTempDirectory() & "doesNotExist.zip" ) )
```

Result: false
