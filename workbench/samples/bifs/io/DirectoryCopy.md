### Simple DirectoryCopy Example

Copy directory from one place to another.


```java
directoryCopy( expandPath( "./mySourceDirectory" ), expandPath( "../MyDestinationDirectory" ) )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6pMzi%2Bo1FBQSoHxCxJLMpR00AUUNK25FPT1FfLzUhVQZBRK8hUS8%2FJLMlKLEBIKIBk9PT0uAG2KJCs%3D" target="_blank">Run Example</a>

```java
directorycopy( "directorypath", "directorypath" )
 // one directorypath to another directory path...

```

### Copy a folder that contains symbolic links

By default a symbolic link is copied as a link that points to the same place.

```java
shared = getTempDirectory() & "linkDemoShared"
build = getTempDirectory() & "linkDemoBuild"
dest = getTempDirectory() & "linkDemoCopy"
directoryCreate( shared )
fileWrite( shared & "/keep.txt", "keep" )
directoryCreate( build )
fileCreateSymlink( build & "/lib", shared )

directoryCopy( build, dest, true )
writeOutput( fileIsSymlink( dest & "/lib" ) )

```

Result: true

### Copy what the links point to

Pass `followLinks=true` to copy the content of the linked folders instead of the link.

```java
directoryDelete( dest, true )
directoryCopy( source=build, destination=dest, recurse=true, followLinks=true )
writeOutput( fileIsSymlink( dest & "/lib" ) & " " & fileExists( dest & "/lib/keep.txt" ) )

```

Result: false true

