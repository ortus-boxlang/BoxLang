### Implicit deletion of the last row

Builds a simple query and removes the last row by not specifying a row index.


```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] )
queryDeleteRow( news )
writeOutput( news[ "title" ][ 1 ] )

```

Result: Dewey defeats Truman

### Deletes a specific row from the query

Builds a simple query and removes one of the rows.

<a href="https://try.boxlang.io/?code=eJxdjjELwjAUhOfkVxyZFLLUUXBzrYK4lQ7BPrVYE01fDEX87z5bHHS7d3ff4zzlHivcE8VhQ3kG0zaWW%2B7IWNGe6UTRPlw8nF0Uq4JWT62U1AyWKOxHT305zZoyDWjoSI577GO6Om%2B0etkfavFHlc4ju%2B7SI3iUIYyIrjHX4641dcS0C7LOy1yLQpIcW6Zt4lviya7w%2FVhX0hD6DYifQBE%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] )
queryDeleteRow( news, 1 )
writeOutput( news[ "title" ][ 1 ] )

```

Result: Man walks on Moon

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1j7EKwjAURefkKx6ZGohC01F0ad2khaKTdEhDxIJaG1OliP%2Fuq6ZDxcXB6UDOI5zb2C6EOTStsV1qbgEwJUqhmQB2VVbvlRUfRLUFSraUEKZCJnqWnjpklBRisNJbTy1HNvLWU0e9pQVw%2BqpJzME4k9fY1GCkAInmZitnkvZ4Dt7JcX1oj6dEOTVcMcWAT129qi4u4P1ntpO%2FL%2FzvwF1tA6iwKpohFnMIkZMJcLhT8mW9FHjG6WO0H5%2Bn%2BTLO8iTONuka%2BBPqDW%2F5" target="_blank">Run Example</a>

```java
qry1 = queryNew( "a,b,c", "varchar,varchar,varchar", [ 
	[
		"a1",
		"b1",
		"c1"
	],
	[
		"a2",
		"b2",
		"c2"
	],
	[
		"a3",
		"b3",
		"c3"
	]
] )
queryDeleteRow( qry1, 2 )
writeDump( queryColumnData( qry1, "a" ).toList() )
qry2 = queryNew( "a,b,c", "varchar,varchar,varchar", [
	[
		"a1",
		"b1",
		"c1"
	],
	[
		"a2",
		"b2",
		"c2"
	],
	[
		"a3",
		"b3",
		"c3"
	]
] )
for( i = 3; i >= 1; i-- ) {
	queryDeleteRow( qry2, i )
}
writeDump( qry2.RECORDCOUNT )

```


