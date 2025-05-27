### Deletes a column from the query

Builds a simple query and removes the 'id' column.

<a href="https://try.boxlang.io/?code=eJxdjjEPgjAQhef2V1w6QdJFR42TTCbAgJtxaOTUxtJqudqg8b9bYNLtvcv3XZ7F2MMGHgH9UGHMQOhWkiaDQqZsCS%2Fo5VP501X5dDoAZ2%2FOWMIErGAhxzzzqYoCIw7Q4hkV9bD3oVNWcPaRP9byzyqVhajMrQdnoXRuUvgR8jWfhhVokHDrTOhsBjZNHrelVwmIXhPWge6BMtg1ddWg18roF84k5CP1BWVRQ84%3D" target="_blank">Run Example</a>

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
queryDeleteColumn( news, "id" );
writeOutput( JSONSerialize( news ) );

```

Result: {"COLUMNS":["TITLE"],"DATA":[["Dewey defeats Truman"],["Man walks on Moon"]]}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFkDFrwzAQRmfpVxw3pXAUrGwNmRIKhTQZ2k4hgy0EMTi1rUiN5NL%2FXiu%2BQlUCnd7wJPE99TYWsITeGxu35jIDLKkijQT4UVp9LC394aj2IMVeCoFlgZRYMXWBUhzoxyq2TK0yO2fL1PNk5QHuFvI6Z20a48yqbfzpfQb9OHQcpTH5i62d2XnXeTeZ%2B9Vu8%2Fa83Ty9vF7v26jyqkCRhn%2BqeFjgmMgcsqjAMZE5ZFGBYyJz%2BBVVhYembbtp1RLTRoRPKW7HKoKmPrvH2p6nRpU1pge%2Fpn9Y%2B1N348RCfgMC83jX" target="_blank">Run Example</a>

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
] );
queryDeleteColumn( qry1, "c" );
writeOutput( qry1.COLUMNLIST );
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
] );
bx:loop query="qry2" {
	queryDeleteColumn( qry2, listFirst( qry2.COLUMNLIST ) );
}
writeDump( qry2.COLUMNLIST );

```


