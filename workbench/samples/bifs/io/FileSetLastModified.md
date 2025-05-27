### Script Syntax




```java
<bx:script>
	fileSetLastModified( "c:	emp	est1.txt", "#now()#" );
	writeOutput( getFileInfo( "c:	emp	est1.txt" ).LASTMODIFIED );
</bx:script>
  
```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFzEEKwjAQQNG9pxiySiBEcOuqEIWCUkEvMDgTHWiTYqa0xzeeQP7285KMXFlHrDoVkiRMFnibMdMN9W3BhL1y1SdWDrqpAeeBULmjNhoyHg4eclmtg9Zxt35EOS7TbOHFmhovOZV%2FJrhw6e6P6xD7c3%2BKP%2BgLmnEw%2FA%3D%3D" target="_blank">Run Example</a>

```java
filesetlastmodified( expandPath( "./testcase.txt" ), dateAdd( "d", 2, now() ) );
writeDump( getfileinfo( expandPath( "./testcase.txt" ) ).LASTMODIFIED );

```


