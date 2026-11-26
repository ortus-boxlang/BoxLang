### Show the temp directory path

This is the file system path where temporary files are stored. The value depends on the operating system and the environment.

```java
writeOutput( getTempDirectory() )
```

Result: an absolute path string such as /tmp/ (environment dependent)

### Write a temporary file

```java
tempFile = getTempDirectory() & createUUID() & ".txt"
fileWrite( tempFile, "hello" )
writeOutput( fileExists( tempFile ) )
```

Result: true
