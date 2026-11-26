### Read a file into a binary object

Uses expandPath to grab a file in current directory.


```java
binaryContent = fileReadBinary( expandPath( "./file.pdf" ) )

```


### Additional Examples


```java
binaryContent = fileReadBinary( expandPath( "./image.jpg" ) )

```

### Read a text file

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/hello.txt"
fileWrite( file, "Hello World" )
writeOutput( fileRead( file, "utf-8" ) )
directoryDelete( dir, true )
```

Result: Hello World

### Read part of a file through a file object

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/hello.txt"
fileWrite( file, "Hello World" )
fileObject = fileOpen( file )
first = fileRead( fileObject, 5 )
rest = fileRead( fileObject )
fileClose( fileObject )
writeOutput( "[" & first & "][" & rest & "]" )
directoryDelete( dir, true )
```

Result: [Hello][ World]

### fileReadBinary returns binary data

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/hello.txt"
fileWrite( file, "Hello World" )
data = fileReadBinary( file )
writeOutput( isBinary( data ) & " " & data.len() )
directoryDelete( dir, true )
```

Result: true 11
