### Two PDFs and two text files with and without strict mode

Assume that you have a file named test.pdf in temp directory and test.txt in the same folder, and you want to check the MIME type. Here test.txt is a copy of test.pdf with extension renamed to txt.


```java
<bx:script>
	mimeTypes = ""
	mimeTypes = listAppend( mimeTypes, fileGetMimeType( expandPath( "/folder1/test.pdf" ) ) )
	mimeTypes = listAppend( mimeTypes, fileGetMimeType( expandPath( "/folder1/test.pdf" ), false ) )
	mimeTypes = listAppend( mimeTypes, fileGetMimeType( expandPath( "/folder1/test.txt" ) ) )
	mimeTypes = listAppend( mimeTypes, fileGetMimeType( expandPath( "/folder1/test.txt" ), false ) )
	writeOutput( mimeTypes )
</bx:script>

```

Result: application/pdf,application/pdf,text/plain,text/plain

### Additional Examples


```java
file = filegetmimetype( filepath / filename.EXT )
writeDump( file )

```

### Get the MIME type of text and image files

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/notes.txt", "hello" )
fileWrite( dir & "/logo.png", "x" )
writeOutput( fileGetMimeType( dir & "/notes.txt" ) & " " & fileGetMimeType( dir & "/logo.png" ) )
directoryDelete( dir, true )
```

Result: text/plain image/png

### Missing files throw an error unless strict is false

```java
dir = createTempDirectory( prefix="docs-" )
try {
	fileGetMimeType( dir & "/missing.pdf" )
} catch ( any e ) {
	writeOutput( "strict: error / " )
}
writeOutput( fileGetMimeType( dir & "/missing.pdf", false ) )
directoryDelete( dir, true )
```

Result: strict: error / application/pdf
