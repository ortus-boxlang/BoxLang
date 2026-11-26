### Example1

This is Example1

<a href="https://try.boxlang.io/?code=eJyVjzELwjAQhefkVxyZKmTRujooLjoIitChdIj2tIG2sdfEkn9vmop0dbnh%2B%2BC9dx152EDnkPwJhwQEmUGXslUNCglCtxafSPKt6F4pCigHznLO2FKGI47KC84KObFVZDtzm7E0smuFpjSEM7GOItN1rVUzcl7AgpN7uD4sOv%2Bx6Js3tV%2FGgF9cfOzQ9kh2axPoyEuIFRLSoDPSFveueUUFiw%2FSyEe4" target="_blank">Run Example</a>

```java
qry = queryNew( "rowid,name", "integer,varchar", [ 
	[
		1,
		"Jay"
	],
	[
		2,
		"Bob"
	],
	[
		3,
		"Theodore"
	],
	[
		4,
		"William"
	]
] )
rufus = QueryNew( "rowid,name", "integer,varchar", [
	[
		42,
		"Rufus"
	]
] )
queryInsertAt( qry, rufus, 3 )
WriteDump( qry )

```


### Member function version.

Using the member function.

<a href="https://try.boxlang.io/?code=eJyVjjELwjAQhefmVxyZWgiC1tVBcdFBUIQOpUO0pw20jb0mlvx701Skq8sN33t89zpysIHOIrkTDjFw0oMqRSsb5AK4ag0%2BkcRb0r2S5FEOLMpZFC2FP%2FwoHWdRISa2CmynbzOWBnatUJeacBasQ5CpulayGTkrIGFkH7b3i85%2FLPr6pu%2BXUfDTdeQWh7ZHMlsTQ5ALSH2QkTK4t80rBt%2BB5AMZsEVk" target="_blank">Run Example</a>

```java
qry = queryNew( "rowid,name", "integer,varchar", [ 
	[
		1,
		"Jay"
	],
	[
		2,
		"Bob"
	],
	[
		3,
		"Theodore"
	],
	[
		4,
		"William"
	]
] )
rufus = QueryNew( "rowid,name", "integer,varchar", [
	[
		42,
		"Rufus"
	]
] )
qry.InsertAt( rufus, 3 )
WriteDump( qry )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJyV0D0LwjAQgOE5%2BRVHphYy2CqCgkPFRQdFilPpUOT6MTRtroml%2F96WWCi46BKOh%2BPliKYhgANoizRcsfdAqKxGCVmBQoJ4ZfQsM5JQKYMF0mgJcJZwxkRsu0rIcQpXnKXyow%2FqHG4WGNeVKR0HC740pXK6HZWn4HNNQ%2FjnPXMMlTsn2M2xniqDN2taa8bQEfOGEO5T%2Bqw6JAN7MW%2BdbN16oKff8PliJTJO5fSGEtZf2Sg3SD9U3%2Bx9Y4s%3D" target="_blank">Run Example</a>

```java
qry1 = queryNew( "name, age", "varchar, integer", [ 
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
qry2 = queryNew( "name, age", "varchar, integer", [
	[
		"Jeni",
		19
	]
] )
writeOutput( "Before QueryInsert :" )
writeDump( qry1 )
QueryInsertAt( qry1, qry2, 3 )
writeOutput( "After QueryInsert :" )
writeDump( qry1 )

```

### Insert rows after the first row

```java
a = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
b = queryNew( "id,name", "integer,varchar", [ [ 9, "Zed" ] ] )
queryInsertAt( a, b, 1 )
writeOutput( arrayToList( queryColumnData( a, "name" ) ) )

```

Result: Ann,Zed,Bob

### Member function inserting at the beginning

```java
a = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
b = queryNew( "id,name", "integer,varchar", [ [ 9, "Zed" ] ] )
a.insertAt( b, 0 )
writeOutput( arrayToList( a.columnData( "name" ) ) )

```

Result: Zed,Ann,Bob

### Queries with different columns

```java
a = queryNew( "id,name" )
b = queryNew( "id,title" )
try {
	queryInsertAt( a, b, 0 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Query columns do not match
