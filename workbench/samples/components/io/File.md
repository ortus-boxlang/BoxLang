### Script Syntax - Write

File Write

<a href="https://try.boxlang.io/?code=eJxLy8xJDS%2FKLEnVUEitKEjMSwlILMnQUFDS08%2BtdAPK6ZVUlCgpaOooKHmkFqWqFysU5%2BemKiTn55Wk5pUopOUXKeRWKqSBFAJVWXMBAC4DGa0%3D" target="_blank">Run Example</a>

```java
fileWrite( expandPath( "./myFile.txt" ), "Here's some content for my file." );

```

Result: 

### Script Syntax - Append

File Append - There is no fileAppend() so we access the file and use fileWriteLine()

<a href="https://try.boxlang.io/?code=eJzLrXTLzElVsFVIA1L%2BBal5GgqpFQWJeSkBiSUZGgpKevq5YBV6JRUlSgqaOgpKiQVAVSlAtjUXSE94UWZJqk9mXqqGAkQlUIlHalGqerFCcX5uqkJearlCcn5eSWpeiR5ck3NOfjFcA0gQAMNXK50%3D" target="_blank">Run Example</a>

```java
myFile = fileOpen( expandPath( "./myFile.txt" ), "append" );
fileWriteLine( myFile, "Here's some new content." );
fileClose( myFile );

```

Result: 

### Script Syntax - Read

File Read

<a href="https://try.boxlang.io/?code=eJzLrXTLzElVsFVIA1JBqYkpGgqpFQWJeSkBiSUZGgpKevq5YBV6JRUlSgqaCprWXAC%2FwxAJ" target="_blank">Run Example</a>

```java
myFile = fileRead( expandPath( "./myFile.txt" ) );

```

Result: Here's some content for my file.
Here's some new content.

### Script Syntax - Read Binary

File Read Binary


```java
myImageBinary = fileReadBinary( expandPath( "./myImage.jpg" ) );

```

Result: 

### Script Syntax - Rename

File Rename - Since there is no fileRename(), fileMove() works just as well


```java
fileMove( expandPath( "./myFile.txt" ), expandPath( "./myNewFileName.txt" ) );

```

Result: 

### Script Syntax - Copy

File Copy


```java
fileCopy( expandPath( "./myFile.txt" ), expandPath( "./some/other/path" ) );

```

Result: 

### Script Syntax - Move

File Move


```java
fileMove( expandPath( "./myFile.txt" ), expandPath( "./some/other/path" ) );

```

Result: 

### Script Syntax - Delete

File Delete

<a href="https://try.boxlang.io/?code=eJxLy8xJdUnNSS1J1VBIrShIzEsJSCzJ0FBQ0tPPrXQDSuqVVJQoKWgqaFpzAQBZxQ39" target="_blank">Run Example</a>

```java
fileDelete( expandPath( "./myFile.txt" ) );

```

Result: 

### Tag Syntax (action=write)

Write the contents of a variable to a file.


```java
<bx:file action="write" file="#expandPath( "./myFile.txt" )#" output="Here's some content for my file.">
```

Result: 

### Tag Syntax (action=append)

Append content to the end of a file.


```java
<bx:file action="append" file="#expandPath( "./myFile.txt" )#" attributes="normal" output="Here's some new content.">
```

Result: 

### Tag Syntax (action=read)

Read a file into a variable


```java
<bx:file action="read" file="#expandPath( "./myFile.txt" )#" variable="myFile">
```

Result: 

### Tag Syntax (action=readBinary)

File Read Binary


```java
<bx:file action="readBinary" file="#expandPath( "./myImage.jpg" )#" variable="myImageBinary">
```

Result: 

### Tag Syntax (action=rename)

Rename a file


```java
<bx:file action="rename" source="#expandPath( "./myFile.txt" )#" destination="#expandPath( "./myNewFileName.txt" )#" attributes="normal">
```

Result: 

### Tag Syntax (action=copy)

Copy a file


```java
<bx:file action="copy" source="#expandPath( "./myFile.txt" )#" destination="#expandPath( "./some/other/path" )#">
```

Result: 

### Tag Syntax (action=move)

Move a file


```java
<bx:file action="move" source="#expandPath( "./myFile.txt" )#" destination="#expandPath( "./some/other/path" )#">
```

Result: 

### Tag Syntax (action=delete)

Delete a file


```java
<bx:file action="delete" file="#expandPath( "./myFile.txt" )#">
```

Result: 

### Tag Syntax (action=upload)

Upload the file contained in the myFile field. Always upload to a directory outside of the webroot, validate the file extension, file content and then only if necessary copy it back to the web root.


```java
<bx:file action="upload" destination="#getTempDirectory()#" filefield="form.myFile" nameconflict="makeunique">
```

Result: 

### Tag Syntax (action=upload) with accept

CF10+ Checks file extensions against a whitelist of allowed file extensions. You must set `strict=false` when specifying a file extension list.


```java
<bx:file action="upload" accept=".png,.jpg" strict="false" destination="#getTempDirectory()#" filefield="form.myFile" nameconflict="makeunique">
```

Result: 

### Tag Syntax (action=uploadall)

Upload all files in the form scope.


```java
<bx:file action="uploadall" destination="#getTempDirectory()#" nameconflict="makeunique">
```

Result: 

