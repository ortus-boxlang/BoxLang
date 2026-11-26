### Script Syntax




```java
<bx:script>
	fileSetLastModified( "c:	emp	est1.txt", "#now()#" )
	writeOutput( getFileInfo( "c:	emp	est1.txt" ).LASTMODIFIED )
</bx:script>
  
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFzEEKwjAQQNG9pxiySiBEcOuqEIWCUkEvMDgTHWiTYqa0xzeeQP7285KMXFlHrDoVkiRMFnibMdMN9W3BhL1y1SdWDrqpAeeBULmjNhoyHg4eclmtg9Zxt35EOS7TbOHFmhovOZV%2FJrhw6e6P6xD7c3%2BKP%2BgLmnEw%2FA%3D%3D" target="_blank">Run Example</a>

```java
filesetlastmodified( expandPath( "./testcase.txt" ), dateAdd( "d", 2, now() ) )
writeDump( getfileinfo( expandPath( "./testcase.txt" ) ).LASTMODIFIED )

```

### Set the last modified date from a string

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/a.txt"
fileWrite( file, "a" )
fileSetLastModified( file, "2020-01-15 10:30:00" )
writeOutput( dateFormat( getFileInfo( file ).lastModified, "yyyy-MM-dd" ) )
directoryDelete( dir, true )
```

Result: 2020-01-15

### Set the last modified date from a date object

```java
dir = createTempDirectory( prefix="docs-" )
file = dir & "/a.txt"
fileWrite( file, "a" )
fileSetLastModified( file, createDateTime( 2021, 6, 1, 8, 0, 0 ) )
writeOutput( dateFormat( fileInfo( file ).lastModified, "yyyy-MM-dd" ) )
directoryDelete( dir, true )
```

Result: 2021-06-01
