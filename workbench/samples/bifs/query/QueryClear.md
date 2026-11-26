### queryClear member function

Description of the code example

<a href="https://try.boxlang.io/?code=eJx90MFKw0AQBuBz9il%2B9pTAolg9iXooBcFDS7E38bBJftxQu6mT3YYgvrubFg9pwdvM8M0MM47SssMjviJlWLLPoZvaLO2O2qTQB35QzMFK5ayk0htU9q2yLCmNe9yYMT7ylOm5xIqYW%2B8pWmU%2FZoJnU7xp%2FYDXYGV7SW%2FP5rZlOWAhdstLeze1L7Qez8JhlOodheqlCVzFsI8hnbceL0XfBAdh1UrdPZSC6yf9Jxdxt8%2FhTp857944okurTg8zKGNA9UkrrP8Zc3UkeYHiF1z5Z%2BI%3D" target="_blank">Run Example</a>

```java
heroes = queryNew( "id,Name", "integer,varchar", [ 
	{
		"id" : 1,
		"Name" : "Bruce Banner"
	},
	{
		"id" : 2,
		"Name" : "Tony Stark"
	},
	{
		"id" : 3,
		"Name" : "Bobby Drake"
	},
	{
		"id" : 4,
		"Name" : "Jean Grey"
	}
] )
writeOutput( "Query with records<br />" )
writeDump( heroes )
writeOutput( "The same query, but cleared<br />" )
writeDump( heroes.clear() )

```

Result: A query with 4 heroes, then a query with none

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFzz0LwkAMBuD57leEm1q4QUUcBAc%2FJgdFilPpcEjaHvQzzVn67z2thTq5hScv4U1LA%2BygdUjDBfsAVGVK1GAyVBrU09AjN6TBVowZkrcYpIilECpynVXaT6uFFIn%2B6p26EdczjErL%2BcjLGZ%2FrvBp141UmEMqeLOPVcePYdzlgWhPC7d3uWKAh2KopdHJlE0Dr64eSsHMFT398ktPq9%2BA%2BZaS%2F917WSUyy" target="_blank">Run Example</a>

```java
qry = queryNew( "name, age", "varchar, integer", [ 
	[
		"Susi",
		20
	],
	[
		"Urs",
		24
	],
	[
		"Smith",
		21
	],
	[
		"John",
		26
	]
] )
writeOutput( "Before QueryClear :" )
writeDump( qry )
result = queryClear( qry )
writeOutput( "After QueryClear :" )
writeDump( qry )

```

### Clear a query but keep its columns

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ] ] )
queryClear( q )
writeOutput( queryRecordCount( q ) & " rows, columns: " & queryColumnList( q ) )

```

Result: 0 rows, columns: id
