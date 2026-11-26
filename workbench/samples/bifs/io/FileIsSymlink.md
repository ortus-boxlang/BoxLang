### Test if a path is a symbolic link

Returns true for a link even if its target does not exist. Regular files, folders and missing paths return false.

```java
base = getTempDirectory() & "linkDemo"
directoryCreate( base & "/releases/v2" )
fileWrite( base & "/releases/v2/app.txt", "app" )
fileCreateSymlink( base & "/current", base & "/releases/v2" )

writeOutput( fileIsSymlink( base & "/current" ) & "," )
writeOutput( fileIsSymlink( base & "/releases/v2/app.txt" ) & "," )
writeOutput( fileIsSymlink( base & "/missing" ) )

```

Result: true,false,false

