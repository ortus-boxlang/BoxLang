### Builds a simple query using queryNew

Using Script with the queryAddRow querySetCell functions to populate the query.

<a href="https://try.boxlang.io/?code=eJydjTEKAjEURHtP8UmlkEZbsRC3XQv1AsGMGswm698fw97ejYtaCWI3DG%2FmBeSOVnRL4H6LPCXlrBYnHkoPOQjOYH03fLwYVjRbTp7o2tpdHOhQ5q9yD9nA%2B7Eta1s%2B5uor8PZUyOjJ4gQjHR04NSb8JVv8IKsRKBt%2FpRiojnEUZXaCKjXtR%2FMADRpalA%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" );
queryAddRow( news );
querySetCell( news, "id", "1" );
querySetCell( news, "title", "Dewey defeats Truman" );
queryAddRow( news );
querySetCell( news, "id", "2" );
querySetCell( news, "title", "Men walk on Moon" );
writeDump( news );

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

CF10+ Passes an array of structs to create a new query.

<a href="https://try.boxlang.io/?code=eJxdjjELwjAUhOfkVxyZKmTRUXHrWic3cXjYpwbbtL4khiL%2Bd1OddLs7vg%2FOcw7Y4p5Yph3nCsa1NrrYsbEl%2B8gXFvsgOV1JynSAVk%2BtVMEM1ljaOX%2F5Uk3NmSe0fGaKAXtJPXmj1cv%2BWKs%2FqyGPTN0tYPBohuGj6CMWG53FRa5TP1bw89UyvQEwGzJ5" target="_blank">Run Example</a>

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
] );
writeDump( news );

```


### Creating and Populating a single row query using rowData in queryNew

CF10+ If you only need one row you can pass a single struct instead of an array into the rowData argument.

<a href="https://try.boxlang.io/?code=eJwljbEKwkAQRGvvK4atIlxjq9iltvIHDjPRA3PoZi9HEP%2FdDXZvHg%2BmsM04412p64Wtg%2BQhWrYnJToX450al6S3R1JXH4SdJ4IjDtHxn%2FqSno0rBo5MNuOqdUpFwhf7U2iajX2dXh3K9ufqB%2BGSJOY%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", { 
	"id" : 1,
	"title" : "Dewey defeats Truman"
} );
writeDump( news );

```


### Creating and populating a query with an array of structs

CF2018u5+ Directly assigns columns and values with an array of structs.

<a href="https://try.boxlang.io/?code=eJzLSy0vVrBVKCxNLar0Sy3XUIhW4OKs5uLkVMpMUVKwUjDUAbFLMktyUkFcJZfU8tRKhZTUtNTEkmKFkKLS3MQ8JS7OWh0UXUZounwT8xTKE3OyixXy8xR88%2FPBWrhiFTStucqLMktSXUpzCzQU8kBuAQoBAJfXKD0%3D" target="_blank">Run Example</a>

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
] );
writeDump( news );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrSS0uCSxNLapUsFUoBNF%2BqeUaCkp5ibmpCjoKiempSjoKSmWJRckZiUVAgbzS3NSizGSgYLUCF6efo6%2BrgpVCNBcnp1JwaXGmkg6IFVpUrMTFGQtkO7rDpI0MQFJGJkBxrloFTWuulNLcAg2FErjtQKFC0h0BNBrVciMDiM3RMIcgrI1FWFtIiYXVQBOhHkfYC%2FEpyPZaHVQlMEdAVQCdUovFKQBBOmMg" target="_blank">Run Example</a>

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
} );
dump( testQuery );
qry = queryNew( "name , age", "varchar , numeric", [
	[
		"Susi",
		20
	],
	[
		"Urs",
		24
	]
] );
dump( qry );
qry = queryNew( "name , age", "varchar , numeric", [
	{
		NAME : "Susi",
		AGE : 20
	},
	{
		NAME : "Urs",
		AGE : 24
	}
] );
dump( qry );

```


