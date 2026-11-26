### The simple queryGetRow example

Here we've a example to get the particular row value from myquery.


```java
<bx:set myQuery = queryNew( "id,name,age", "integer,varchar,integer", [ 
	[
		1,
		"Dharshini",
		20
		],
	[
		2,
		"Subash",
		18
		]
	] ) >
<bx:dump var="#queryrowdata( myQuery, 2 )#"/>
```


### The simple getRow example

We've example to get the particular row value from myquery using script syntax.


```java
<bx:script>
	var myQuery = queryNew( "id,title,author", "integer,varchar,varchar", [
		[
			1,
			"Charlottes Web",
			"E.B. White"
		],
		[
			3,
			"The Outsiders",
			"S.E. Hinton"
		],
		[
			4,
			"Mieko and the Fifth Treasure",
			"Eleanor Coerr"
		]
	] )
	writeDump( myQuery.getRow( 3 ) )
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVdIBMvNKdMoSi5IzEouUFDS5CvUSU1KC8oGKTMC84tQS59ScHLAeoHJDIMImbgRE2MSNgQibuIkOhvkwB%2BXngSh0W2CyJeX5Sph2wWUzilJBDHQ7YfJp%2BaVFShCrU0pzCzQUCuGsQFDoAD3ukliSCBQH26GgCQA2VlZ8" target="_blank">Run Example</a>

```java
q = queryNew( "id,name", "int,varchar" )
q.addRow( 4 )
q.setCell( "id", 1, 1 )
q.setCell( "id", 2, 2 )
q.setCell( "id", 3, 3 )
q.setCell( "id", 4, 4 )
q.setCell( "name", "one", 1 )
q.setCell( "name", "two", 2 )
q.setCell( "name", "three", 3 )
q.setCell( "name", "four", 4 )
dump( q )
dump( QueryRowData( q, 2 ) )

```

### Get a row as a struct

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
row = queryRowData( q, 2 )
writeOutput( row.id & ":" & row.name )

```

Result: 2:Bob

### Member function

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
writeOutput( q.rowData( 1 ).name )

```

Result: Ann

### Row outside the query

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
try {
	queryRowData( q, 5 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Row number outside of query row range.
