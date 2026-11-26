### An array of files in this directory



<a href="https://try.boxlang.io/?code=eJxLLCpKrPRP88lPTsxxy8xJLVawVUjJLEpNLskvqvTJLC7RUEitKEjMSwlILMnQUFDS01dS0NRRSEvMKU7VUVDKS8xNBQpYcwEAo5oXPg%3D%3D" target="_blank">Run Example</a>

```java
arrayOfLocalFiles = directoryList( expandPath( "./" ), false, "name" )

```

Result: [.DS_Store, .ortus, Application.bx, MyDestinationDirectory, Page.bx, assets, bifs, components, compressed_test.txt.gz, example.bxm, example.bxm, filepath, images, index.bxm, myNewFileName.txt, new, new_directory, server.json, setup_db.sql, some, test.txt, testcase.txt]

### A query of files in this directory sorted by date last modified



<a href="https://try.boxlang.io/?code=eJwrLE0tqvRPc8vMSS1WsFVIySxKTS7JL6r0ySwu0VBIrShIzEsJSCzJ0FBQ0tNXUtDUUUhLzClO1VFQKgTpVAIyQNglsSTVJ7G4xDc%2FJTMtMzVFwcU12Bmo3JoLAELdHpE%3D" target="_blank">Run Example</a>

```java
queryOfFiles = directoryList( expandPath( "./" ), false, "query", "", "DateLastModified DESC" )

```

### An array of files in the temp directory

Including sub-directories and as an array containing full paths

<a href="https://try.boxlang.io/?code=eJxLLCpKrPRPC0nNLXDLzEktVrBVSMksSk0uyS%2Bq9MksLtFQUNLTV9JRKCkqTVXQtOYCAKoOD88%3D" target="_blank">Run Example</a>

```java
arrayOfTempFiles = directoryList( "./", true )

```


### Filter files with closure

Pass a closure instead of a string as `filter` param

<a href="https://try.boxlang.io/?code=eJwljUsKg0AQRNfxFI0rA8NcIBiQRFf5QG7Qji1O0Bnp7gQl5O4ZyaaKgno8ZMb13jd%2BVGLqUpNACZ1nchp5vXjRAnKbG%2BhxFDKQB5wozQKqsMKMOsAeyiN8sh2TvjjAxjQ%2BdLd4QqFEV%2FM8eofqY7DtYji2UcXqokaI38T2KTGYHt%2FepUcKYwdF50jEPOrqfK3t1CXlX3bIvlv8AAO8Pew%3D" target="_blank">Run Example</a>

```java
arrayOfFilteredFiles = directoryList( ".", false, "name", ( Any path ) => {
	return ListFindNoCase( "Application.bx,robots.txt,server.json,favicon.ico,.htaccess,README.md", path )
} )

```

Result: []

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6r0ySwu0VBQ0i9LLNJPSSxJVNJRSEvMKU7VUVAqSCzJAHI1FBzzKhVAHAVNBVs7hWouzqLUktKiPIXEovTS3NS8kmK9AMcQD72MxOLg0rS0zAqgeXo5%2BelKCprWXLUgAgDHRiJT" target="_blank">Run Example</a>

```java
directoryList( "/var/data", false, "path", ( Any path ) => {
	return arguments.PATH.hasSuffix( ".log" )
} )

```

Result: []

### Symbolic links in a recursive listing

A recursive listing does not descend into symbolic links by default. Pass `followLinks=true` to list what the links point to. A link given as the `path` itself is always resolved.

```java
base = getTempDirectory() & "linkDemo"
directoryCreate( base & "/releases/v2" )
fileWrite( base & "/releases/v2/app.txt", "app" )
fileCreateSymlink( base & "/current", base & "/releases/v2" )

names = directoryList( path=base, recurse=true, listInfo="name" )
writeOutput( names.len() & " " & names.toList() )

```

Result: 4 current,releases,v2,app.txt

```java
names = directoryList( path=base, recurse=true, listInfo="name", followLinks=true )
writeOutput( names.len() & " " & names.toList() )

```

Result: 5 current,app.txt,releases,v2,app.txt

### Find the links with the query columns

The query returned by `listInfo="query"` has the `isLink` and `linkTarget` columns. `linkTarget` is an empty string for items that are not links.

```java
q = directoryList( base, false, "query" )
for ( row in q ) {
	writeOutput( row.name & " isLink=" & row.isLink & " target=" & row.linkTarget.listLast( "/" ) & "; " )
}

```

Result: current isLink=true target=v2; releases isLink=false target=;

### Filter to only the links

`type` keeps its values of `all`, `file` and `dir`. To list only links, use a `filter` closure with `fileIsSymlink()`.

```java
links = directoryList( path=base, listInfo="name", filter=( path ) => fileIsSymlink( path ) )
writeOutput( links.toList() )

```

Result: current

