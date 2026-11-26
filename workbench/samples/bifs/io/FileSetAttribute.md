### Create a temporary file and then change read-only mode



<a href="https://try.boxlang.io/?code=eJzLrXTLzElVsFVITy0JSc0tAPE0YByXzKLU5JL8okoNTR0FpZLU4hKQtJKCpjVXeVFmSap%2FaUlBaYmGglJmsQJIIDEpJ9VKQUlBDWQASKlnXlq%2BhkIuxA5NPWdHv%2FAgzxBXkAFpQKHg1BLHkpKizKTSklSYMqBFRamJKf55OZVYLFJ41DaJSAsARH5HRA%3D%3D" target="_blank">Run Example</a>

```java
myFile = getTempFile( getTempDirectory(), "testFile" )
writeOutput( "is writable: " & getFileInfo( myFile ).CANWRITE )
fileSetAttribute( myFile, "readOnly" )
writeOutput( " → " & getFileInfo( myFile ).CANWRITE )

```

Result: is writable: YES → NO

### Additional Examples


```java
filesetattribute( "example.txt", "readonly" )

```

### Invalid attributes throw an error

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/a.txt"
fileWrite( file, "a" )
try {
	fileSetAttribute( file, "bogus" )
} catch ( any e ) {
	writeOutput( e.message )
}
directoryDelete( dir, true )
```

Result: The attribute provided [bogus] is not valid for this method.

### A file object is returned when one is passed

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/a.txt"
fileWrite( file, "a" )
fileObject = fileOpen( file )
result = fileSetAttribute( fileObject, "normal" )
writeOutput( isFileObject( result ) & " " & isNull( fileSetAttribute( file, "normal" ) ) )
fileClose( fileObject )
directoryDelete( dir, true )
```

Result: true true
