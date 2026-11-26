### Deletes a column from the query

Builds a simple query and removes the 'id' column.

<a href="https://try.boxlang.io/?code=eJxdjjEPwiAQhWf4FRemkrDoaOJkJxPboW7GgdhTiRSUHpJq%2FO%2FSdtLtvcv3XZ7D1MMaHhHDUGEqQJhWkSGLQuXsCC8Y1FOH01WHfDoAZ2%2FOWMYErGChxjzzuYoSEw7Q4hk19bAPsdNOcPZRP9byz9ppB0nbWw%2Fewc77SeFHkHzaVaJFwo23sXMFuLx4nJY%2FSZ6CIawj3SMVsG3qqsFgtDUvnEGQIL9iJUMT" target="_blank">Run Example</a>

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
queryDeleteColumn( news, "id" )
writeOutput( JSONSerialize( news ) )

```

Result: {"COLUMNS":["TITLE"],"DATA":[["Dewey defeats Truman"],["Man walks on Moon"]]}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFkDFrwzAQRmfpVxw3JXAUrGyFTAmFQpoMbaeQwRaCGpzaVqRGcsh%2Fr1xfKCqBTm94kvieehsLWELvjY1bc54BllSRRgL8Kq3%2BKC39YVJ7kGIvhcCyQBpZMXWBUhzoZhVbplaZXbBl6sVo5QHm8mfN2jTGmVXb%2BOPnDPq0M23SmPTZ1s7svOu8m8TDard5f9lunl%2Ffxts2qjwpUKThnyReFbgkMoesKHBJZA5ZUeCSyBx%2Bi6rw2LRtN41a4jgR4SLF%2FVJF0NQn91Tb01SossL03nX6hLU%2FdncOfAPnwXdr" target="_blank">Run Example</a>

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
queryDeleteColumn( qry1, "c" )
writeOutput( qry1.COLUMNLIST )
qry2 = queryNew( "x,y,z", "varchar,varchar,varchar", [
	[
		"x1",
		"y1",
		"z1"
	],
	[
		"x2",
		"y2",
		"z2"
	],
	[
		"x3",
		"y3",
		"z3"
	]
] )
bx:loop query="qry2" {
	queryDeleteColumn( qry2, listFirst( qry2.COLUMNLIST ) )
}
writeDump( qry2.COLUMNLIST )

```

### Member function

```java
q = queryNew( "a,b,c" )
q.deleteColumn( "b" )
writeOutput( queryColumnList( q ) )

```

Result: a,c

### Column does not exist

```java
q = queryNew( "a,b" )
try {
	queryDeleteColumn( q, "z" )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Column 'z' does not exist in the query.
