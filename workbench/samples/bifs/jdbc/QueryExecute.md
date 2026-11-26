### Simple Example

SQL only example. Assumes that a default datasource has been specified (by setting the variable `this.datasource` in Application.bx).

```java
qryResult = queryExecute("SELECT * FROM Employees")
```

### Using Named Placeholders

Use `:structKeyName` in your sql along with a struct of key/value pairs:

```java
qryResult = queryExecute(
  "SELECT * FROM Employees WHERE empid = :empid AND country = :country", 
  {
    country="USA", 
    empid=1
  }
)
```

### Using Positional Placeholders

You can pass placeholders by position using an array of parameters and the question mark `?` symbol:

```java
qryResult = queryExecute(
  "SELECT * FROM Employees WHERE empid = ? AND country = ?", 
  [
    1,
    "USA"
  ]
)
```

### Return Type Options

By default, `queryExecute()` will return a [query object](../../types/query) with the results of the SQL statement. To return results as an array of structs, you can specify the `returnType: "array"`:

```java
queryExecute("SELECT * FROM Employees", [], { returnType: "array" })
```

```json
[
	{
		"empid": 1,
		"firstname": "John",
		"lastname": "Doe",
		"country": "USA"
	},
	{
		"empid": 2,
		"firstname": "Jane",
		"lastname": "Smith",
		"country": "Canada"
	}
]
```

You can also return results as a key-value struct by specifying the `returnType` and `columnKey` options:

```java
queryExecute("SELECT * FROM Employees", [], { returnType: "struct", columnKey: "empid" })
```

```json
{
	"1": {
		"empid": 1,
		"firstname": "John",
		"lastname": "Doe",
		"country": "USA"
	},
	"2": {
		"empid": 2,
		"firstname": "Jane",
		"lastname": "Smith",
		"country": "Canada"
	}
}
```

### Caching a query

You can enable query caching by setting `cache` to `true`. By default, the cached query will never expire, but you can use `cacheTimeout` to specify a timeout duration via the [createTimespan](../temporal/CreateTimeSpan) BIF.

```java
qryResult = queryExecute(
  "SELECT * FROM Employees", 
  [],
  {
    "cache": true,
    "cacheTimeout": createTimespan( 0, 0, 0, 2 )
  }
)
```

This will mark the query for cache eviction after 2 seconds. You can also use `cacheLastAccessTimeout` to specify a timeout based on the last access time of the query:

```java
qryResult = queryExecute(
  "SELECT * FROM Employees", 
  [],
  {
    "cache": true,
    "cacheLastAccessTimeout": createTimespan( 0, 0, 0, 2 )
  }
)
```

See our [caching documentation](https://boxlang.ortusbooks.com/boxlang-framework/caching) for more information on caching.


### Query of Queries

Query a local database variable without going through your database

<a href="https://try.boxlang.io/?code=eJxNzLEKwkAMBuD58hQhUys%2BgeAiFpzEQQcRh%2FNMseC1mrtYpdy7e3WQDoEf8v%2BfBpaAS3wqy2fLfYFUNxJiaz3THOllxd2s5HhCMAMYM%2FnjAmljWwKT4IwlBL0cpl71ZqeRsxn4zi7iDGvpPOpYyuSQ8oFZr%2FbHXTVivxVBylYvTeSr%2BkeBf7b8ApCtNlk%3D" target="_blank">Run Example</a>

```java
users = queryNew( "firstname", "varchar", [ 
	{
		"firstname" : "Han"
	}
] )
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query"
} )
writedump( subUsers )

```


### Return Query as an Array of Structs - Boxlang5+

Return a query object converted into an array of structs.

<a href="https://try.boxlang.io/?code=eJxNjcEKgkAQhs87TzHMScMnCLpEQicR0UNEh81GElqr2d1MxHdvDQoPA8P8%2F3yftywWN%2Fj0LEPGfYTUtGJdpw1TgvTSUl%2B1hPWIoEZQapHjGmmvOwI1wQljsP5cLXnpm2vvODAt37h2uMJG7gb9XArIcQoDarctD3k6w75flIAq0rIqst9Zi%2BiBYAqKXlrHF28eEf5t8QeSCTyX" target="_blank">Run Example</a>

```java
users = queryNew( "firstname", "varchar", [ 
	{
		"firstname" : "Han"
	}
] )
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query",
	RETURNTYPE : "array"
} )
writedump( subUsers )

```

Result: [ { firstname: "Han" } ]

### Return Query as a Struct of Structs

Return a query object converted into a struct of structs. (Struct key is based on the "columnkey" parameter)

<a href="https://try.boxlang.io/?code=eJxNjk0LgkAQhs%2B7v2LYk4WXrkGXSggqC9GDRAfTsYS0mt3NQvzvzQZ9HAaGh%2FfLaiQNE7hZpGeIrQeqKnwoK9KmyWpUPoPG4BHJh3tG%2BSkjZjuQopNCsFjBGEa%2B%2B38mRmqRNUqKXu5hILU9JP9FwQNza5DLNJ4xNzCEki41WCfi%2BK7nk2I%2BjdNt4MLeLsUtURAnUfjB2pDNjeOzzSpZh8sgdZhHyZ5rW6oMFra%2BevBdMHgBby5H9Q%3D%3D" target="_blank">Run Example</a>

```java
users = queryNew( "id, firstname", "integer, varchar", [ 
	{
		"id" : 1,
		"firstname" : "Han"
	}
] )
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query",
	RETURNTYPE : "struct",
	COLUMNKEY : "id"
} )
writedump( subUsers )

```

Result: { 1: { id: 1, firstname: "Han" } }

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNj82LwjAQxc%2BZv2LIqS65rEehh3XN4sEv6hciUoId3UJta5Kqi%2FR%2FN03R9TLvMW8e%2FCa2ZCyGeK5I%2F03oGiCP00RgnBM1cij0utBZwgXyNLd0JC3wovT%2BV%2F0bF24R2BYY%2BxRu8DJTueXeqqNOObCdaPOuXxp1oTa%2BKku6yWGHHfAUEZkqezHJG%2B0rSwGacxbyuRzJ7wV%2B4E80HWPs4ddDGckW2JV6jTqgUml1MuEd2ETKAfbQObb6Gi2l82%2BAi83Mb56vAKuhFliUNi1y3x%2F0nzceiEPtSJPqVAb4ztt5AGPfXrk%3D" target="_blank">Run Example</a>

```java
_test = queryNew( "_id, _need, _forWorld", "integer, varchar, varchar", [ 
	[
		1,
		"plant",
		"agri"
	],
	[
		2,
		"save",
		"water"
	]
] )
queryResult = queryExecute( sql="SELECT * FROM _test WHERE _need = :need", params={
	NEED : {
		VALUE : "plant",
		TYPE : "varchar"
	}
}, options={
	DBTYPE : "query"
} )
dump( queryResult )

```



```java
queryExecute( sql="insert into user (name) values (:name)", params={ 
	NAME : {
		VALUE : "boxlang",
		TYPE : "varchar"
	}
}, options={
	DBTYPE : "query",
	RESULT : "insertResult"
} )
dump( insertResult.GENERATEDKEY )

```

### Query of queries with a named parameter

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
result = queryExecute( "SELECT name FROM users WHERE id = :id", { id: "2" }, { dbtype: "query" } )
writeOutput( result.name )
```

Result: Leia

### Positional parameters and an array return type

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
names = queryExecute(
	"SELECT id, name FROM users WHERE id <> ? ORDER BY name DESC",
	[ "2" ],
	{ dbtype: "query", returnType: "array" }
)
writeOutput( names.len() & " " & names[ 1 ].name )
```

Result: 2 Luke

### Typed list parameters

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
result = queryExecute(
	"SELECT name FROM users WHERE id IN (:ids) ORDER BY id",
	{ ids: { value: "1,3", type: "varchar", list: true } },
	{ dbtype: "query" }
)
writeOutput( queryColumnData( result, "name" ).toList() )
```

Result: Han,Luke

### Convert the results with a transformer

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
total = queryExecute(
	"SELECT * FROM users",
	[],
	{ dbtype: "query", transformer: ( qry, meta ) -> qry.recordCount }
)
writeOutput( total )
```

Result: 3

### Unknown return types throw an error

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
try {
	queryExecute( "SELECT * FROM users", [], { dbtype: "query", returnType: "bogus" } )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Unknown return type: bogus
