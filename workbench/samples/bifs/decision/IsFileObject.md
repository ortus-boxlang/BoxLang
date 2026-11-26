### Check for a filestream-object

Checks if the fileObj is a filestream-object.

```java
filePath = getTempFile( getTempDirectory(), "isfile" )
fileObj = fileOpen( filePath )
writeOutput( isFileObject( fileObj ) )
fileClose( fileObj )
fileDelete( filePath )
```

Result: true

### A file path is not a file object

```java
writeOutput( isFileObject( "/tmp/myfile.txt" ) )
```

Result: false
