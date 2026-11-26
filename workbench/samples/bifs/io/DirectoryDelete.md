### Tag Syntax




```java
<bx:set delDirectory = "someDirPath" >
<bx:set recurse = true >
<bx:set directoryDelete( delDirectory, recurse ) >
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6pMSc1JLUnVUFBygYkEJJZkKCloAgDlzQxg" target="_blank">Run Example</a>

```java
directorydelete( "DirectoryPath" )

```

### Delete a folder that contains symbolic links

A recursive delete never follows symbolic links. A link is removed as a link and the folder it points to is left alone.

```java
shared = getTempDirectory() & "linkDemoShared"
build = getTempDirectory() & "linkDemoBuild"
directoryCreate( shared )
fileWrite( shared & "/keep.txt", "keep" )
directoryCreate( build )
fileWrite( build & "/out.txt", "out" )
fileCreateSymlink( build & "/lib", shared )

directoryDelete( build, true )
writeOutput( directoryExists( build ) & " " & fileExists( shared & "/keep.txt" ) )

```

Result: false true

### Delete a symbolic link given as the path

Only the link is removed, with or without `recursive` and even when `followLinks` is true.

```java
fileCreateSymlink( build, shared )
directoryDelete( build, true )
writeOutput( fileIsSymlink( build ) & " " & fileExists( shared & "/keep.txt" ) )

```

Result: false true

### Follow symbolic links when deleting

Pass `followLinks=true` to delete the contents of the folders that links inside the directory point to as well. Use with care, the targets are emptied. A link that leads back into the folder being deleted, or to a folder above it, is removed as a link.

```java
directoryCreate( build )
fileCreateSymlink( build & "/lib", shared )

directoryDelete( build, true, true )
writeOutput( directoryExists( build ) & " " & fileExists( shared & "/keep.txt" ) )

```

Result: false false

