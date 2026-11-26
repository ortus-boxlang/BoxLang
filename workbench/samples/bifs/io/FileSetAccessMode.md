### Grant read access to everyone




```java
<bx:script>
	fileSetAccessMode( "test1.txt", "004" )
</bx:script>
   
```


### Additional Examples


```java
fileSetAccessMode( "sample_test.txt", "004" )

```

### Make a file executable

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/run.sh"
fileWrite( file, "echo hi" )
fileSetAccessMode( file, "755" )
writeOutput( getFileInfo( file ).canExecute )
fileSetAccessMode( file, "644" )
writeOutput( " " & getFileInfo( file ).canExecute )
directoryDelete( dir, true )
```

Result: true false

### A file object is returned when one is passed

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/run.sh"
fileWrite( file, "echo hi" )
fileObject = fileOpen( file )
result = fileSetAccessMode( fileObject, "644" )
writeOutput( isFileObject( result ) & " " & isNull( fileSetAccessMode( file, "644" ) ) )
fileClose( fileObject )
directoryDelete( dir, true )
```

Result: true true
