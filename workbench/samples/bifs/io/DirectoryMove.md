### Tag Syntax




```java
<bx:set renameDirectory = "someDirPath" >
<bx:set rename = "newName" >
<bx:set directoryRename( renameDirectory, newName ) >
```


### Additional Examples


```java
reNamedirectory = "directoryPath"
directoryRename( reNamedirectory, "newname" )

```

### Move a directory

```java
dir = createTempDirectory( prefix="docs-" )
directoryCreate( dir & "/old/inner" )
directoryMove( dir & "/old", dir & "/new" )
writeOutput( directoryExists( dir & "/old" ) & " " & directoryExists( dir & "/new/inner" ) )
directoryDelete( dir, true )
```

Result: false true

### Rename a directory using the directoryRename alias

```java
dir = createTempDirectory( prefix="docs-" )
directoryCreate( dir & "/draft" )
directoryRename( dir & "/draft", dir & "/final" )
writeOutput( directoryExists( dir & "/draft" ) & " " & directoryExists( dir & "/final" ) )
directoryDelete( dir, true )
```

Result: false true
