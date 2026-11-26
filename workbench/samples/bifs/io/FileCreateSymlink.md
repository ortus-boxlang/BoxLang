### Point a link at a folder

Create `current` as a symbolic link to the `releases/v2` folder.

```java
base = getTempDirectory() & "linkDemo"
directoryCreate( base & "/releases/v2" )
fileCreateSymlink( base & "/current", base & "/releases/v2" )
writeOutput( fileIsSymlink( base & "/current" ) )

```

Result: true

### Use a relative target and replace an existing link

The target is stored exactly as given, so a relative target is relative to the folder that holds the link. Pass `overwrite=true` to replace an existing file or link. Folders are never replaced.

```java
fileCreateSymlink( link=base & "/current", target="releases/v2", overwrite=true )
writeOutput( fileReadSymlink( base & "/current" ) )

```

Result: releases/v2

### The call throws if the path already exists

```java
fileCreateSymlink( base & "/current", "releases/v1" )

```

Result: throws "The path [/tmp/linkDemo/current] already exists and the overwrite argument is set to false."

