### Move file from here to there




```java
fileMove( sourcefile, destinationfile )

```


### Additional Examples


```java
filemove( sourceFilePath, destinationFilePath )

```

### Move a file

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/old.txt", "data" )
fileMove( dir & "/old.txt", dir & "/new.txt" )
writeOutput( fileExists( dir & "/old.txt" ) & " " & fileExists( dir & "/new.txt" ) )
directoryDelete( dir, true )
```

Result: false true

### Move a file into a directory

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/report.txt", "data" )
directoryCreate( dir & "/archive" )
fileMove( dir & "/report.txt", dir & "/archive" )
writeOutput( fileExists( dir & "/archive/report.txt" ) )
directoryDelete( dir, true )
```

Result: true
