### The simple querygetcell example

Here we've example about querygetcell. We created query using queryNew() then we got the last title column value using querygetCell. If we give row number displays the particular row title value otherwise it displays last row value.


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
<bx:output>#querygetcell( myQuery, "name" )#</bx:output>
```

Result: Anil

### The simple querygetcell (getCell) script based example

Here we've the example to get particular column(title) value in script syntax


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
	writeOutput( myQuery.getCell( "title", 2 ) );
</bx:script>

```

Result: The Outsiders

### Additional Examples

