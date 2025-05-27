### Simple QueryCurrentRow Example

Here we've example to get the currentRow number.


```java
<bx:set myQuery = queryNew( "id,name", "integer,varchar", [ 
	[
		1,
		"Rajesh"
		],
	[
		2,
		"Anil"
		]
	] ) >
<bx:loop query="myQuery">
	<bx:if name == "Anil" >
		<bx:output>#queryCurrentRow( myQuery )#</bx:output>
	</bx:if>
</bx:loop>
```

Result: 2

### Simple currentRow Example

Here we've example to get the currentRow number from query using script syntax.


```java
<bx:script>
	var myQuery = queryNew( "id,title", "integer,varchar", [
		[
			1,
			"Charlottes Web"
		],
		[
			3,
			"The Outsiders"
		],
		[
			4,
			"Mieko and the Fifth Treasure"
		]
	] );
	bx:loop query="myQuery" {
		if( title == "Mieko and the Fifth Treasure" ) {
			writeOutput( myQuery.currentRow() );
		}
	}
</bx:script>

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwljrEOwjAMROf4KyxPrZS1Q0GdYGFB6sJSdQhgQQVJi0lUIsS%2Fk7TLDc9357OxlYgNtoElHnkukIardsYyaaSD83xj0Scju7uRhDoE1YFSda2T0tsSqF6vrKoWZh%2BZQY%2FlFs6fzXMcJ3zl%2BoZs%2Fkb4BTXL4Hkf7FSst0sQYedlTAsWF5Y5%2F4M%2FB6Itlg%3D%3D" target="_blank">Run Example</a>

```java
myQry = QueryNew( "id,name", "Integer,VarChar", [ 
	[
		99,
		"sm"
	],
	[
		55,
		"mk"
	]
] );
bx:loop query="myQry" {
	writeDump( querycurrentrow( myQry ) );
}

```


