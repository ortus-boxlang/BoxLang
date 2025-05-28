### Maps query results

Manipulates query column

<a href="https://try.boxlang.io/?code=eJxlj0FLxDAQhc%2FJr3jkIF0Iwnq09NY9LNgVoeBBPMTd6RrdpmuaGIrsf3dSZUU9Zebjvbw3jtKICm%2BR%2FLShVEDZnQ42HEhpnl2gPXn9bvz22XhGD5DiQwrBMoVrLHWev%2FS8qpoSTdhRRyaMaH3sjVNSnPQv19UfV2Mckjm8jhgcmmGYLfIRi1ImbwPVsT8WcLkqI9lFtw2Wpb053uXiBYybcPv0ggU4xnZFXi7XNaoKS4Z5a9ftzYpPVZvVPWfi4oeWUngK0buMSnmSHDV%2FzPL5bcx3vj5n%2Fi93xp8Zw2OR" target="_blank">Run Example</a>

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

function mapQuery( any Obj ) {
	if( Obj.ID == 1 ) Obj.TITLE = "NEW: " & Obj.TITLE;
	return Obj;
}
newQuery = QueryMap( news, mapQuery );
writeDump( newQuery );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1UE1Lw0AQPWd%2FxWNPCQ6SCNaGUEENHgQrIp5CD2kz1YD5cJIYgvjfnZUKIukyC7Mz7828fS037RtjhceBZVrz6MPWecVUNFvKX9gS7Ecuu9dcqMh7prLutZbBeJnxPPs0dKUlzW6EtZ3q9RHFFyEh0kDgeqHxNnQgPEs3g4%2FPj%2BFvhYsZwuLogruymsEvl%2F%2FxZoMgMcKdfv7dff4%2Bb320P34QfFzVE6QZ6TdZD9WW5fDkXSNFxz0CrC7xaTwFZLDOMGx0oFublvu9ujnpUccUcJo%2BXBP%2B6joLowVBI9ZBAU4QJTqK%2B0FqR0jMl9M4SqmEoVJ5Tq5WvgF9r3EJ" target="_blank">Run Example</a>

```java
people = QueryNew( "name,dob,age", "varchar,date,int", [ 
	[
		"Susi",
		CreateDate( 1970, 1, 1 ),
		0
	],
	[
		"Urs",
		CreateDate( 1995, 1, 1 ),
		0
	],
	[
		"Fred",
		CreateDate( 1960, 1, 1 ),
		0
	],
	[
		"Jim",
		CreateDate( 1988, 1, 1 ),
		0
	]
] );
res = queryMap( people, ( Any row, Any rowNumber, Any recordset ) => {
	row[ "age" ] = DateDiff( "yyyy", row.DOB, CreateDate( 2016, 6, 9 ) ) + 1;
	return row;
} );
writeDump( res );

```


