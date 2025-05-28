### An array of files in this directory



<a href="https://try.boxlang.io/?code=eJxLLCpKrPRP88lPTsxxy8xJLVawVUjJLEpNLskvqvTJLC7RUEitKEjMSwlILMnQUFDS01dS0NRRSEvMKU7VUVDKS8xNBQpYcwEAo5oXPg%3D%3D" target="_blank">Run Example</a>

```java
arrayOfLocalFiles = directoryList( expandPath( "./" ), false, "name" );

```

Result: [.DS_Store, .ortus, Application.bx, MyDestinationDirectory, Page.bx, assets, bifs, components, compressed_test.txt.gz, example.bxm, example.bxm, filepath, images, index.bxm, myNewFileName.txt, new, new_directory, server.json, setup_db.sql, some, test.txt, testcase.txt]

### A query of files in this directory sorted by date last modified



<a href="https://try.boxlang.io/?code=eJwrLE0tqvRPc8vMSS1WsFVIySxKTS7JL6r0ySwu0VBIrShIzEsJSCzJ0FBQ0tNXUtDUUUhLzClO1VFQKgTpVAIyQNglsSTVJ7G4xDc%2FJTMtMzVFwcU12Bmo3JoLAELdHpE%3D" target="_blank">Run Example</a>

```java
queryOfFiles = directoryList( expandPath( "./" ), false, "query", "", "DateLastModified DESC" );

```

### An array of files in the temp directory

Including sub-directories and as an array containing full paths

<a href="https://try.boxlang.io/?code=eJxLLCpKrPRPC0nNLXDLzEktVrBVSMksSk0uyS%2Bq9MksLtFQUNLTV9JRKCkqTVXQtOYCAKoOD88%3D" target="_blank">Run Example</a>

```java
arrayOfTempFiles = directoryList( "./", true );

```


### Filter files with closure

Pass a closure instead of a string as `filter` param

<a href="https://try.boxlang.io/?code=eJwljU0KwjAQhdf2FENXFUIuIBWK2pU%2F4A3GZEojbVJmxmIR726Km%2Fd48D4%2BZMbl1rVhUGLyuUmgBh%2BYnCZezkG0gtKWBjochAyUEUfKs4ImLjCh9rCFeg%2BfYsOkL46wMm2I%2FpoOKJTpZpqG4FBDitZ1znB6JBWrbzVCPBPbp6RoOpyDy5ccxvaKzpGIuZ%2Ba4%2BVkR5%2Bdf9uu%2BK7xA0D6Pj4%3D" target="_blank">Run Example</a>

```java
arrayOfFilteredFiles = directoryList( ".", false, "name", ( Any path ) => {
	return ListFindNoCase( "Application.bx,robots.txt,server.json,favicon.ico,.htaccess,README.md", path );
} );

```

Result: []

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6r0ySwu0VBQ0i9LLNJPSSxJVNJRSEvMKU7VUVAqSCzJAHI1FBzzKhVAHAVNBVs7hWouzqLUktKiPIXEovTS3NS8kmK9AMcQD72MxOLg0rS0zAqgeXo5%2BelKCprWXLUgAgDHRiJT" target="_blank">Run Example</a>

```java
directoryList( "/var/data", false, "path", ( Any path ) => {
	return arguments.PATH.hasSuffix( ".log" );
} );

```

Result: []

