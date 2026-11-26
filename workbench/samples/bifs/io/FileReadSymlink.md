### Read where a link points

Returns the target exactly as it is stored in the link. Throws an exception if the path is not a symbolic link.

```java
base = getTempDirectory() & "linkDemo"
directoryCreate( base & "/releases/v2" )
fileCreateSymlink( base & "/current", "releases/v2" )
writeOutput( fileReadSymlink( base & "/current" ) )

```

Result: releases/v2

### Resolve the target to an absolute path

With `resolve=true` a relative target is resolved against the folder that holds the link and the path is normalized.

```java
resolved = fileReadSymlink( path=base & "/current", resolve=true )
writeOutput( resolved == base & "/releases/v2" )

```

Result: true

