### Builds a simple query using queryNew and queryAppend

Add new query to the end of the current query using queryAppend.

<a href="https://try.boxlang.io/?code=eJzLS8xNLVawVSgsTS2q9Est11BQSs0tyInPTNHJA0op6SgoZeaVpKanFumUJRYlZyQWAYWiFbg4o7k4OWFKlRSsFAyNjC11QGJgbUABJa%2F8jDwlLs5YrlgFTa681PI8sqzCYpOpgSGaTYl5qXCbwMY7FhSk5qVoKIDt1FGA267JVV6UWZLqUppbAJVU0AQA4N1MgQ%3D%3D" target="_blank">Run Example</a>

```java
names = queryNew( "empl_id,name", "integer,varchar", [ 
	[
		"empl_id" : 1239,
		"name" : "John"
	]
] )
newnames = queryNew( "empl_id,name", "integer,varchar", [
	[
		"empl_id" : 1501,
		"name" : "Jane"
	]
] )
queryAppend( names, newnames )
writeDump( names )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJydjrEKwjAQhufkKY6bWuhQipPgUFAEwYKok3QI9bAZEmuaWER8d68txc5Od3z%2F8X%2FnqfWHQO4FK3j0s6AuArTKECSgboQJ4FO5qlaOgQ2GnK4YvkGKIt9vYAkXKQQeQ6sx6beza1GKkvd8O8VZ2kfZgrn8QCwtdad%2Fzdw3Go329aDM0tE34N29tvizlWwb6vOmIXuNwE9e7px%2FEcvOaU%2FrYJrZEcRf73VQsg%3D%3D" target="_blank">Run Example</a>

```java
testQuery = queryNew( "name , age", "varchar , numeric", { 
	NAME : [
		"Susi",
		"Urs"
	],
	AGE : [
		20,
		24
	]
} )
newTestQuery = queryNew( "name , age", "varchar , numeric", [
	[
		"Smith",
		20
	],
	[
		"John",
		24
	]
] )
queryAppend( testQuery, newTestQuery )
writeDump( testQuery )

```

### Append the rows of another query

```java
a = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ] ] )
b = queryNew( "id,name", "integer,varchar", [ [ 2, "Bob" ], [ 3, "Cy" ] ] )
queryAppend( a, b )
writeOutput( queryRecordCount( a ) & ": " & arrayToList( queryColumnData( a, "name" ) ) )

```

Result: 3: Ann,Bob,Cy

### Member function returns the first query

```java
a = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ] ] )
b = queryNew( "id,name", "integer,varchar", [ [ 2, "Bob" ] ] )
c = a.append( b )
writeOutput( queryRecordCount( c ) & " " & queryRecordCount( b ) )

```

Result: 2 1 (the second query is not modified)
