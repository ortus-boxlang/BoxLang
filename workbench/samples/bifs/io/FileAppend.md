### Simple Example

Appends a mock entry to a file.


```java
// Create mock log entry
logEntry = dateTimeFormat( now(), "yyyy/mm/dd HH:nn" ) & " this is a mock log entry!"
// Append line to file
fileAppend( "/path/to/file.log", logEntry )

```


### Additional Examples


```java
fileAppend( "path/to/file", "new content to append" )

```

### Append to a file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/log.txt"
fileWrite( file, "Hello" )
fileAppend( file, " World" )
writeOutput( fileRead( file ) )
directoryDelete( dir, true )
```

Result: Hello World

### Add a line separator after the appended data

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/log.txt"
fileWrite( file, "first" )
fileAppend( file, " entry", "utf-8", true )
fileAppend( file, "second entry" )
fileObject = fileOpen( file )
lines = []
while ( !fileIsEOF( fileObject ) ) {
	lines.append( fileReadLine( fileObject ) )
}
fileClose( fileObject )
writeOutput( lines.toList( "|" ) )
directoryDelete( dir, true )
```

Result: first entry|second entry
