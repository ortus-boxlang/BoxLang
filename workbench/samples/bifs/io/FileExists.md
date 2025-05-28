### Checks if file at the given path exists.




```java
var myFile = "/path/to/the/file.jpg";
if( fileExists( expandPath( myFile ) ) ) {
	writeOutput( myFile & "exists!" );
}

```


### Additional Examples


```java
var filePath = "path/to/my/file.md";
if( fileExists( filePath ) ) echo;
"it exists";

```


