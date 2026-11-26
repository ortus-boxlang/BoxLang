### getCanonicalPath Example

 Returns the canonical path of the input path.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSE8tcU7My8%2FLTE7MCUgsyQCLOCUWp4ak5hbkJJakggU1FYAQAGEPFAk%3D" target="_blank">Run Example</a>

```java
writeOutput( getCanonicalPath( getBaseTemplatePath() ) )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLSixOjS9ILMlQsFVITy1xAnJDUnMLchJLUgOAohqaXMmJefl5mcmJORAVzjAuWFohCa5fkyulNLdAQwGhXpNLQV9fQb8ssUi%2FJLE4GwD%2BXiSx" target="_blank">Run Example</a>

```java
base_path = getBaseTemplatePath()
canonical = getCanonicalPath( base_path )
dump( canonical )
 // /var/task

```

### Resolve relative path elements

```java
dir = createTempDirectory( prefix="docs-" )
fileWrite( dir & "/a.txt", "a" )
directoryCreate( dir & "/sub" )
canonical = getCanonicalPath( dir & "/sub/../a.txt" )
writeOutput( canonical == getCanonicalPath( dir & "/a.txt" ) )
directoryDelete( dir, true )
```

Result: true

### Directories end with a separator and missing paths are returned as given

```java
dir = createTempDirectory( prefix="docs-" )
writeOutput( getCanonicalPath( dir ).endsWith( "/" ) & " " & getCanonicalPath( "/no/such/path" ) )
directoryDelete( dir, true )
```

Result: true /no/such/path
