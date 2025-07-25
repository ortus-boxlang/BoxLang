### Simple Example

SQL only example. Assumes that a default datasource has been specified (by setting the variable `this.datasource` in Application.bx).

```java
qryResult = queryExecute("SELECT * FROM Employees");
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
);
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
);
```

### Return Type Options

By default, `queryExecute()` will return a [query object](../../types/query) with the results of the SQL statement. To return results as an array of structs, you can specify the `returnType: "array"`:

```java
queryExecute("SELECT * FROM Employees", [], { returnType: "array" });
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
queryExecute("SELECT * FROM Employees", [], { returnType: "struct", columnKey: "empid" });
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
);
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
);
```

See our [caching documentation](https://boxlang.ortusbooks.com/boxlang-framework/caching) for more information on caching.


### Query of Queries

Query a local database variable without going through your database

<a href="https://try.boxlang.io/?code=eJxNzLEKwkAMBuD58hQhUxWfQHERC07FQQcRh%2FNMsWCr5i5WKffu3jlIh8BPkv9Tz%2BJxiU9l%2BVTcF0h1Iz50tmWaIb2suKuVFI8IZgBjRnecI21sR2AinHCyAK%2Fn%2FRgs3%2Bw0cEI939gFnGIt9xY1PyVziGnArFe7w7bM2q9FEDPWSxP4ou2jwL%2Bb1l9gIzcU" target="_blank">Run Example</a>

```java
users = queryNew( "firstname", "varchar", [ 
	{
		"firstname" : "Han"
	}
] );
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query"
} );
writedump( subUsers );

```


### Return Query as an Array of Structs - Boxlang5+

Return a query object converted into an array of structs.

<a href="https://try.boxlang.io/?code=eJxNjUELgkAQhc87v2KYk4W%2FoOgSCZ0kRA8RHTYbSXCtZnczEf97a1B4GHi89%2BZ73rJY3ODTs%2FQpdxFSVYt1rTZMMdJLS3nTEuQJQQ2g1CzHFdJetwRqhDMu1mD9pZgDkzeX3nGAWm64dLjESu4G%2FVQKzGEMB2q3zY%2BHZKJ9vygGlSV5kaU%2FW4vonmCcNjqpHV%2B9eUT4nwv2B3mGPVI%3D" target="_blank">Run Example</a>

```java
users = queryNew( "firstname", "varchar", [ 
	{
		"firstname" : "Han"
	}
] );
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query",
	RETURNTYPE : "array"
} );
writedump( subUsers );

```

Result: [ { firstname: "Han" } ]

### Return Query as an Array of Structs - Boxlang 4.5

Return a query object converted into an array of structs.


```java
users = queryNew( "firstname", "varchar", [ 
	{
		"firstname" : "Han"
	}
] );
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query",
	RETURNTYPE : "array-of-entities"
} );
writedump( subUsers );

```

Result: [ { firstname: "Han" } ]

### Return Query as a Struct of Structs

Return a query object converted into a struct of structs. (Struct key is based on the "columnkey" parameter)

<a href="https://try.boxlang.io/?code=eJxNjk0LgkAQhs%2B7v2LYk4WXrkWXSggqC9FDRAfTsYS0mt3NQvzvzQZ9HAaGh%2FfLaiQNY7hZpGeIjQeqzH0oStKmTitUPoPa4BHJh3tK2SklZjuQopVCsFjBEAa%2B%2B38mRmqe1kqKTu6hN5LaHpL%2FpuCBmTXIbRrPmBnoQ0GXCqwTcX7b8Ukxm8TbTeDS3i7FNVEQJ1H4wdqQzYzj0%2FUyWYWLYOswr5Kd622oNJjb6urBdwLjF4O4SLA%3D" target="_blank">Run Example</a>

```java
users = queryNew( "id, firstname", "integer, varchar", [ 
	{
		"id" : 1,
		"firstname" : "Han"
	}
] );
subUsers = queryExecute( "select * from users", {}, {
	DBTYPE : "query",
	RETURNTYPE : "struct",
	COLUMNKEY : "id"
} );
writedump( subUsers );

```

Result: { 1: { id: 1, firstname: "Han" } }

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNj09rwkAQxc87n2LYUyp70aMlh6orPagtqVVEQljMaAMxibsbbSn57m42%2BOcy7%2FHeDPwmsWQshniqSf8t6BIgT7JUYFIQtbIv9brUecoF8qywdCAt8Kz07kc9jCu3CGwLjPWFG7zKVWG5t%2BqgMw4sFl0%2F8KFRZ%2Brqi7Kk2x5ifHkFjxGRqfM7lPylXW0pQHPKQ%2F4lZ3K8xB5Oo485Jp5%2B%2FS4j2RG7o2GrjqhSWh1N%2BA9sIeUEh%2BgcW73NvqXzT4TLzadPbr8Aa6ARWFY2Kwt%2FPxnddjwQh6ZFTetjFeAzsAuv0S5fdA%3D%3D" target="_blank">Run Example</a>

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
] );
queryResult = queryExecute( sql="SELECT * FROM _test WHERE _need = :need", params={
	NEED : {
		VALUE : "plant",
		TYPE : "varchar"
	}
}, options={
	DBTYPE : "query"
} );
dump( queryResult );

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
} );
dump( insertResult.GENERATEDKEY );

```


