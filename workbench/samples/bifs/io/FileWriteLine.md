### Script Syntax




```java
myfile = fileOpen( "c:\temp\test1.txt", "write" )
fileWriteLine( myfile, "This line is new." )
fileClose( myfile )

```


### Additional Examples


```java
openFile = fileopen( filepath, "read" )
readfromfile = filereadline( openfile )
filewriteline( filepath, readfromfile )

```

### Write lines to a new file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
fileObject = fileOpen( file, "write" )
fileWriteLine( fileObject, "This line is new." )
fileWriteLine( fileObject, "So is this one." )
fileClose( fileObject )
reader = fileOpen( file )
writeOutput( fileReadLine( reader ) & " / " & fileReadLine( reader ) )
fileClose( reader )
directoryDelete( dir, true )
```

Result: This line is new. / So is this one.

### Using the member function

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/lines.txt"
fileObject = fileOpen( file, "write" )
fileObject.writeLine( "member line" )
fileObject.close()
writeOutput( fileRead( file ).trim() )
directoryDelete( dir, true )
```

Result: member line
