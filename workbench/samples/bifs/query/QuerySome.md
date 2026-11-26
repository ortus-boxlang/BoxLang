###  The simple Querysome example

Here,we've example to check whether the 75 is exists or not in myquery mark column value.


```java
<bx:script>
	myQuery = queryNew( "id,name,mark", "integer,varchar,integer", [
		[
			1,
			"Rahu",
			75
		],
		[
			2,
			"Ravi",
			80
		]
	] )
	result = querySome( myQuery, ( Any details ) => {
		return details.MARK == 75
	} )
	writeOutput( (result ? "Some" : "No") & " matches  Record found!" )
</bx:script>

```

Result: Some matches Record found!

### The Query Member Function example

Here,we've example to check whether the 85 is exists or not in myquery mark column value using query member function.


```java
<bx:script>
	myQuery = queryNew( "id,name,mark", "integer,varchar,integer", [
		[
			1,
			"Rahu",
			75
		],
		[
			2,
			"Ravi",
			80
		]
	] )
	result = myQuery.Some( ( Any details ) => {
		return details.MARK == 85
	} )
	writeOutput( (result ? "Some" : "No") & " matches  Record found!" )
</bx:script>

```

Result: No matches Record found!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNkE9Lw0AQxc%2FZT%2FHIoezCUpKD2oIpqMGDh4gUT6WHrZnWheZPp0lDEL%2B7E6sXScFlF2Z3fm9n5tVU1XtCgpeWuM%2Bo0whLV5DNq411OwotwpPjt3fHNncNWV828raCClYqCMJle%2FShleiBSdKpHI14fhNZxLJhhlykgrX9EbzycYSfX13iH5nyEcH1xQJPvhjhZ7O%2FvFrDqJPb%2B1ymPwzTL6tC2PrbEQuNu7IHV539DbK22BCfrwfu5WsHg2SBDxUwNS2X0HoomPrtVnzsZYlXopymz%2FcWWdVpI4oFIoPJBP9hbxPEUWSM%2BpRuO%2FYiaIta49y4%2BQKqUnOR" target="_blank">Run Example</a>

```java
people = QueryNew( "name,dob,age", "varchar,date,int", [ 
	[
		"Susi",
		CreateDate( 1970, 1, 1 ),
		0
	],
	[
		"Urs",
		CreateDate( 1995, 1, 1 ),
		0
	],
	[
		"Fred",
		CreateDate( 1960, 1, 1 ),
		0
	],
	[
		"Jim",
		CreateDate( 1988, 1, 1 ),
		0
	]
] )
valid = querySome( people, ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 0) && (DateDiff( "yyyy", row.DOB, Now() ) <= 100))
} )
writeDump( valid )

```


