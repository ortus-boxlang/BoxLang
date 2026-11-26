### Script Syntax




```java
<bx:script>
	fileSetLastModified( "c:	emp	est1.txt", "#now()#" )
	writeOutput( getFileInfo( "c:	emp	est1.txt" ).LASTMODIFIED )
</bx:script>
  
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFzEEKwjAQRuG9p%2FjJKoEQwRsUolBQFPQCwZnoQJsUM9Ie39xA3vbxZZm4sU6p6VxJsjBZ8LakQrekbwsT9spNn6lx0E0NnAcl5YH6aMh4HDxKXa1Db7d%2BRDl%2B58XixZq7LiXXfyRcOA%2F3x%2BUax9N4jHA%2FDnkwfA%3D%3D" target="_blank">Run Example</a>

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
