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
	] )
	bx:loop query="myQuery" {
		if( title == "Mieko and the Fifth Treasure" ) {
			writeOutput( myQuery.currentRow() )
		}
	}
</bx:script>

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsqlSwVQgsTS2q9Est11BQykzRyUvMTVXSUVDyzCtJTU8t0glLLHLOSCwCCkUrcHFGc3FyWlrqAEml4lwlLs5YHYiYqSlYLDcbJMYVq6DJlVRhlZOfX6BQCDLdVikXZJmSQjUXZ3lRZkmqS2lugQZELrm0qCg1r6QoH%2BgAsCoFTaD2WgCG0i0W" target="_blank">Run Example</a>

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
] )
bx:loop query="myQry" {
	writeDump( querycurrentrow( myQry ) )
}

```


