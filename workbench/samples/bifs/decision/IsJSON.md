### isJSON Example

Returns true when the argument is a valid JSON value.

<a href="https://try.boxlang.io/?code=eJzLLPYK9vfTUFCKNtQx0jGOVVLQBAA0QASS" target="_blank">Run Example</a>

```java
isJSON( "[1,2,3]" )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJydj7FOwzAQhufkKU6eUskDSUFiyQB06lCEwlZlOJcTteQk7TkhCk%2FPuS1SCckAki1bv%2FV9%2F7ln29Kqqw4JODTkcvXAjIPS8IGcW7%2F2TZ2A2qY608tSwQIWcf%2BLKWz97gjqrjLEQrqORoZ0jmxZ2ElE%2Bc7bNFve3s3WXsHQ23YPntiis5%2B0Lp43I1%2BIiu%2F3i%2F5knnQ%2FNo0jrP8pb1mgOfUTE0ok%2B9VWf3f%2FxBPIbtJ7Den1OldL%2BZEHyOHYEQ8b6uXTiKiNMdKgpGK3Rz51hYtk2ziSFSlUOhxGxVGpL9nunL2FLC4nP%2FYSekbDhwlkki9w7L9q" target="_blank">Run Example</a>

```java
writeDump( label="Array", var=isJson( "[1,2,3]" ) )
writeDump( label="Single number value", var=isJson( 1 ) )
writeDump( label="String value", var=isJson( "susi12345" ) )
writeDump( label="String value with serializeJSON", var=isJson( JSONSerialize( "susi" ) ) )
writeDump( label="Boolean value with serializeJSON", var=isJson( JSONSerialize( true ) ) )
writeDump( label="CreateDateTime with serializeJSON", var=isJson( JSONSerialize( CreateDateTime( 2018, 1, 1, 1, 1, 1 ) ) ) )
qry = queryNew( "aaa,bbb", "varchar, varchar", [
	[
		"a",
		"b"
	],
	[
		"c",
		"d"
	]
] )
writeDump( label="Query", var=isJson( qry ) )

```



### Test objects, arrays and plain text

```java
writeOutput( isJSON( '{"name":"BoxLang","version":1}' ) & " " & isJSON( "not json" ) & " " & isJSON( "{name:" ) )
```

Result: true false false
