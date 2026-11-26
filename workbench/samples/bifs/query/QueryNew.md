### Builds a simple query using queryNew

Using Script with the queryAddRow querySetCell functions to populate the query.

<a href="https://try.boxlang.io/?code=eJydjTsOwjAQRHtOsXIFkhvoKRBpQwFcwMIDRDhr2Kyxcnvi8OkQEt1ofo%2BRO1rSLUH6DfKUTOOtNhpg7KBZcYLYu5PD2Ymh2WRsrrzfxqHMZf3ydtA1QniaZevLw9x8yz%2BQChk9eRzhtKO9pNbxH6TFb1INpuzChSJTHeNIydIoqtRe34wHN2FYsg%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
queryAddRow( news )
querySetCell( news, "id", "1" )
querySetCell( news, "title", "Dewey defeats Truman" )
queryAddRow( news )
querySetCell( news, "id", "2" )
querySetCell( news, "title", "Men walk on Moon" )
writeDump( news )

```


### Builds a simple query using queryNew

Using BL Tags with the queryAddRow querySetCell functions to populate the query.


```java
<bx:set news = queryNew( "id,title", "integer,varchar" ) >
<bx:set queryAddRow( news ) >
<bx:set querySetCell( news, "id", "1" ) >
<bx:set querySetCell( news, "title", "Dewey defeats Truman" ) >
<bx:set queryAddRow( news ) >
<bx:set querySetCell( news, "id", "2" ) >
<bx:set querySetCell( news, "title", "Men walk on Moon" ) >
<bx:set writeDump( news ) >
```


### Creating and Populating a query using an array rowData in queryNew

Passes an array of structs to create a new query.

<a href="https://try.boxlang.io/?code=eJxdjrsKwkAUROvdrxi2SmAbLQW7tLGyE4uLuepicmP24RLEf3ejlXYzwzkwwjlgiymxn3ecKxjX2ehiz8aWLJEv7O2D%2FOlKvkwHaPXUShXMYIOVXfKXL9U0nHlGx2emGLD3aSAxWr3sj7X%2Bs1oSZOpvAaOgHcePoo%2BodfYucpOGewVZntZvmuMx%2BQ%3D%3D" target="_blank">Run Example</a>

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
writeDump( news )

```


### Creating and Populating a single row query using rowData in queryNew

If you only need one row you can pass a single struct instead of an array into the rowData argument.

<a href="https://try.boxlang.io/?code=eJwljbEKwkAQRGvvK4atErjGVrBLbeUPHGaMB%2BbQzZ5HkPy7K%2BneGx5MYVtwxrtS1wtbB8ljtGxPSnQuxokaP0lvj6Q%2BfREOnghOOEbHPXWTgY0rRt6ZbMFV65yKhA19aJqNQ51fHcr%2Frv8BdSIkZg%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", { 
	"id" : 1,
	"title" : "Dewey defeats Truman"
} )
writeDump( news )

```


### Creating and populating a query with an array of structs

Directly assigns columns and values with an array of structs.

<a href="https://try.boxlang.io/?code=eJzLSy0vVrBVKCxNLar0Sy3XUIhW4OKs5uLkVMpMUVKwUjDUAbFLMktyUkFcJZfU8tRKhZTUtNTEkmKFkKLS3MQ8JS7OWh0UXUZounwT8xTKE3OyixXy8xR88%2FPBWrhiFTS5yosyS1JdSnMLNBTyQE7RBAAhYie9" target="_blank">Run Example</a>

```java
news = queryNew( [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] )
writeDump( news )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrSS0uCSxNLapUsFUoBNF%2BqeUaCkp5ibmpCjoKiempSjoKSmWJRckZiUVAgbzS3NSizGSgYLUCF6efo6%2BrgpVCNBcnp1JwaXGmkg6IFVpUrMTFGQtkO7rDpI0MQFJGJkBxrloFTa6U0twCDYUSuOWaXIWkOwFoMKrVRgYQe6NhzkBYGgu3tJAC66qB5kE9jbAV4kuQ3bU6qEpgToCqADqkFt0hAKDRYbQ%3D" target="_blank">Run Example</a>

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
dump( testQuery )
qry = queryNew( "name , age", "varchar , numeric", [
	[
		"Susi",
		20
	],
	[
		"Urs",
		24
	]
] )
dump( qry )
qry = queryNew( "name , age", "varchar , numeric", [
	{
		NAME : "Susi",
		AGE : 20
	},
	{
		NAME : "Urs",
		AGE : 24
	}
] )
dump( qry )

```

### Create an empty query with column types

```java
q = queryNew( "id,name", "integer,varchar" )
writeOutput( queryColumnList( q ) & " / " & queryRecordCount( q ) )

```

Result: id,name / 0

### Create a query from an array of structs

```java
q = queryNew( "id,name", "integer,varchar", [ { id: 1, name: "Ann" }, { id: 2, name: "Bob" } ] )
writeOutput( queryRecordCount( q ) & " rows: " & arrayToList( queryColumnData( q, "name" ) ) )

```

Result: 2 rows: Ann,Bob

### Column names taken from an array of structs

```java
q = queryNew( [ { a: 1, b: 2 } ] )
writeOutput( queryRecordCount( q ) & " row, " & queryColumnCount( q ) & " columns" )

```

Result: 1 row, 2 columns

### Column and type lists differ in length

```java
try {
	queryNew( "a,b", "integer" )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: columnList and columnTypeList must have the same number of elements
