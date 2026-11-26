### isObject Example

Returns true if the parameter is a BL object. The BL object here is a Java object.

<a href="https://try.boxlang.io/?code=eJwrSsxLyc9VsFVILkpNLEn1T8pKTS7RUFDKSixLVNKB0HrFqcmlRZkllXrBIEZqEFiPkoKmXnpqiWdecUliXnIqUE%2Bwh6NhQJCfO1CGqxyoPtW%2FtKSgFGhaZjHM3CKIdZoKmgDUZShn" target="_blank">Run Example</a>

```java
random = createObject( "java", "java.security.SecureRandom" ).getInstance( "SHA1PRNG" )
writeOutput( isObject( random ) )

```

Result: true

### isObject Example for Other Data Types

Returns false if the parameter is any data type other than a BL object

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJVbBVqFbg4lTKS8xNVVKwUlDyVEjMVUhUKC4pKk0uUeKq5SovyixJ9S8tKSgt0VDILPZPykpNBrJSofo1FTQBcqYXxg%3D%3D" target="_blank">Run Example</a>

```java
example = { 
	"name" : "I am a struct"
}
writeOutput( isObject( example ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyVkEEKwjAQRfeeYsiqBQl4gG7UhQjqoieYlmk7JcaSThq8vWkDItRNV7OY%2F9%2BfP8Gx0Nk%2FhwwMVmQKVYpj28KExpPax%2BkKHh9VT7VkoEo%2FsoIc8l1YOb%2ByHAJLB%2BgcvqHxthZ%2B2RVrWd8pZHCYgX%2BRJ0colBzAFq444QpU%2F4jiiX3SLFN7YaMvOHY3HNSmmNSBLcc%2BmyIN2lanLx5905CLuTpx5vgPCeR6vg%3D%3D" target="_blank">Run Example</a>

```java
writeDump( label="String value", var=isObject( "Susi" ) )
writeDump( label="isObject() with array function", var=isObject( arrayNew( 1 ) ) )
writeDump( label="CreateObject in Java", var=isObject( createObject( "java", "java.util.HashMap" ) ) )
writeDump( label="CreateObject in Java with init()", var=isObject( createObject( "java", "java.lang.StringBuffer" ).init() ) )

```


