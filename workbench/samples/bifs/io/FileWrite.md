### Write a Temporary File



<a href="https://try.boxlang.io/?code=eJxLy8xJDS%2FKLEnVUEhPLQlJzS1wA4rAOS6ZRanJJflFlRqaOgpKJVBpJQUQz7dSwSWxJBHIAQBZSBa2" target="_blank">Run Example</a>

```java
fileWrite( getTempFile( getTempDirectory(), "tempFile" ), "My Data" )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLy8xJLS%2FKLEnVUFBKA7ILEksylHQgbOf8vJLUvBKFknyFpFQFsColBU0AxSMRLg%3D%3D" target="_blank">Run Example</a>

```java
filewrite( "filepath", "fileContent to be write" )

```

### Write and read back a file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/greeting.txt"
fileWrite( file, "My Data" )
writeOutput( fileRead( file ) )
directoryDelete( dir, true )
```

Result: My Data

### Create missing directories with createPath

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/reports/2024/summary.txt"
try {
	fileWrite( file, "summary" )
} catch ( any e ) {
	writeOutput( "no directory / " )
}
fileWrite( file, "summary", "utf-8", true )
writeOutput( fileRead( file ) )
directoryDelete( dir, true )
```

Result: no directory / summary
