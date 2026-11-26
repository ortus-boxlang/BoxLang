### Example1

This is Example1

<a href="https://try.boxlang.io/?code=eJxNzjELwjAQBeA5%2BRVHphZuiXZ1EScHBxUcSodoDxtoGnO2hvx7Y4vQ5eB97w0XOMEOwkScThQLUOyjbXEwjhSCssNIT2L8GH50hjPVIEUthdCYj9r7u5KiwcU2s1078q1nWhXbuTiatLJqtpvte2vcz2UDpXxH89L%2Fj84%2BXnIuIHBC0AhVXkS2Ix0ml3kZl19k1zIU" target="_blank">Run Example</a>

```java
qry = queryNew( "rowid,name", "integer,varchar", [ 
	[
		1,
		"Bob"
	],
	[
		2,
		"Theodore"
	],
	[
		3,
		"Jay"
	],
	[
		4,
		"William"
	]
] )
swap1 = queryRowSwap( qry, 1, 4 )
writeDump( swap1 )

```


### Member function version.

Using the member function.

<a href="https://try.boxlang.io/?code=eJxNzjEPgjAQBeC5%2FRWXTpBcTEBWF%2BPE4KKJA2GocpEmlMoJNv33lrKwXHLfe8ObOMAJpoU4XMlnoNh50%2BGoLSkEZcaZ3sT40%2FzqNUdqQIpGClFgPOrsnkqKFjcrk917cp1j2gXHFNQ67KxK9jDDYLRdXbaQy6%2FXn3JdxOEQp9zim0GBUMXMs5npstgoWy3%2FAy8PL%2BI%3D" target="_blank">Run Example</a>

```java
qry = queryNew( "rowid,name", "integer,varchar", [ 
	[
		1,
		"Bob"
	],
	[
		2,
		"Theodore"
	],
	[
		3,
		"Jay"
	],
	[
		4,
		"William"
	]
] )
swap2 = qry.rowSwap( 1, 4 )
writeDump( swap2 )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNzbEKg0AMBuD58hThJoUs1bmvUGg7ikPqBdtB68Xa496%2B0SK4hD8fP%2FxRM54xLqL5IqlA%2Fwo08iCeLI4f6UXpy9o9WY0aBNeAcyey49mDa%2Bkv1SaPg9SbdKtAiyXMiadJgq1d17XbO91NCoyaCSvC2jphGUz25v5bA8sfPNsrTg%3D%3D" target="_blank">Run Example</a>

```java
qry = queryNew( "id,name", "integer,varchar", [ 
	[
		1,
		"a"
	],
	[
		2,
		"b"
	],
	[
		3,
		"c"
	]
] )
swapped = QueryRowSwap( qry, 2, 3 )
dump( swapped )
dump( qry )

```

### Swap the first and last rows

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ] ] )
queryRowSwap( q, 1, 3 )
writeOutput( arrayToList( queryColumnData( q, "id" ) ) )

```

Result: 3,2,1

### Member function

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ] ] )
q.rowSwap( 1, 2 )
writeOutput( arrayToList( q.columnData( "id" ) ) )

```

Result: 2,1,3

### Row out of range

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ] ] )
try {
	queryRowSwap( q, 1, 9 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Row index 8 is out of bounds for query of size 3
