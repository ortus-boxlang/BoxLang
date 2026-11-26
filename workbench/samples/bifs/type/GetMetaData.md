### Dump Metadata of CFC Instance



```java
writeDump( getMetadata( new Query() ) )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVdJRUMorzU0tykzWKUssSs5ILFJS0LTmSinNLdBQcE8t8U0tSUxJLEnUUChU0ATJAABTTRZo" target="_blank">Run Example</a>

```java
q = queryNew( "id,name", "numeric,varchar" )
dump( GetMetadata( q ) )

```


### Metadata of a function

```java
function greet( required string name, numeric times = 1 ) {
	return name
}
meta = getMetadata( greet )
writeOutput( meta.name & " " & meta.parameters.len() )
```

Result: greet 2

### Non-BoxLang values return their Java class

```java
writeOutput( getMetadata( "abc" ).getName() )
```

Result: java.lang.String
