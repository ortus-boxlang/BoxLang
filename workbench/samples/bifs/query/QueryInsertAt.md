### Example1

This is Example1

<a href="https://try.boxlang.io/?code=eJyVjzELwjAQhefkVxyZKmTRuomD4qKDoAgdSodoTxtoG3tNLPn3Nqmgq8sN3wfvvevIwxo6h%2BSPOCQgyAy6lK1qUEgQurX4QJIvRbdK0Yhy4CznjM3leMRBecFZISe2iGxrrj8sjexSoSkN4Y9YRpHputaqCZwXMFtxcnfXj5NOf0z6BE715xDwzYuv7dseyW5sAh15CbFDQhp8RtrizjXP6AJ5Ay0pSK4%3D" target="_blank">Run Example</a>

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
] );
rufus = QueryNew( "rowid,name", "integer,varchar", [
	[
		42,
		"Rufus"
	]
] );
queryInsertAt( qry, rufus, 3 );
WriteDump( qry );

```

Result: 

### Member function version.

Using the member function.

<a href="https://try.boxlang.io/?code=eJyVjj0LwjAURefmVzwyVQiC1k0cFBcdBEXoUDpE%2B7SBftjXxJJ%2Fb5IKdnV5w7mX825HFjbQGSR7wiEGTu2gCtHIGrkArhqNTyTxlnQvJTmUAYsyFkUL4Q4%2FSstZlIuRLQPbtbcJSwK7ltgWLeEkWIUgVVWlZO05y2G2ZmQepneTzn9M%2BgrH9xcv%2BPk6svND0yPprY4h2AUkPklJadyb%2BhWDK3nyAWpCRlo%3D" target="_blank">Run Example</a>

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
] );
rufus = QueryNew( "rowid,name", "integer,varchar", [
	[
		42,
		"Rufus"
	]
] );
qry.InsertAt( rufus, 3 );
WriteDump( qry );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyV0LEKwjAQgOG5eYojU4UMtoqg4lBx0UGR4lQcilzbDE3ba2Lp25sSLQUXXcLxcfwcaagPYAeNQerP2PnAVVqigDRHLoA%2FU3oUKQmQSmOOZC0B5iXM83hsWsmFncI58%2B7irTdqHS4nGJdSF46DCZ%2BqQjldWWV3mG1ZQ33450GfGip3T7Aeax1JjReja6NtaY9ZRQjXoX1ULZKGDR%2FXDqasfWiGD7E0WYq0YzG8oYDFdznKNNJv4RfXX2Uy" target="_blank">Run Example</a>

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
] );
qry2 = queryNew( "name, age", "varchar, integer", [
	[
		"Jeni",
		19
	]
] );
writeOutput( "Before QueryInsert :" );
writeDump( qry1 );
QueryInsertAt( qry1, qry2, 3 );
writeOutput( "After QueryInsert :" );
writeDump( qry1 );

```


