### Dump Metadata of CFC Instance



```java
writeDump( getMetadata( queryNew( "id,name", "integer,varchar" ) ) )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwFwTEKgDAMBdDdU3wyNdArOHfSO4Q2YIcUG1LF2%2FvexI651L9T3wTqLQ8xpQway9R7zY94vcQJvLVld0LRODSkSUjCBIN%2FEjUV6A%3D%3D" target="_blank">Run Example</a>

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
