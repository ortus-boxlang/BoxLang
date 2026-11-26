### Builds a simple query using queryNew and querySetRow

Sets third row of query overwriting news entry

<a href="https://try.boxlang.io/?code=eJxtkL1uQjEMRuebp%2FiUCaQsbWGp1KVihQGQOqAOoRiIGpzi5DZFiHev%2BVm47RI50XeOYzPVjBfsW5LDhGoPNqxcCSWSdVpzoQ2J%2B%2FbysfWiTwuY5miaRmMWz3hw5%2Fqa16sdUaUDVrQmXzLm0u48W9Oc3B312KHGxKg%2BfiIxxin9Qzx1iEn40eyUcthw%2FhsfdOLzLeGVJAbGm48Raz0umHlH31yGn1GZJp2fdSFO%2B0F1N9tQbR3ZeTHCVBAylkn0xycVVQmFRu3u66pB%2Fxe3j2Cd" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Men walk on Moon"
	},
	{
		"id" : 3,
		"title" : "Nixon Resigns"
	},
	{
		"id" : 4,
		"title" : "The Berlin Wall falls"
	}
] )
querySetRow( news, 3, {
	"id" : 5,
	"title" : "The internet is born"
} )
writeDump( news )

```


### Builds a simple query using queryNew and someQuery.setRow member syntax

Sets third row of query overwriting news entry

<a href="https://try.boxlang.io/?code=eJxtkLFuQjEMReeXr7jKBFJUqYUuSF0QKwwIqUPVIRQDEcFpnbymqOLf69ey8Ohi2dE9R46ZasYTPlqS04LqADZsXAklknXac6Edifv08rb3ok8vMM23aRqNWUxw77r%2BL6%2BjnVGlEza0JV8yVtIePVvTnN0V9dCj5sSoPh6QGPOU%2FiFGPWIRvjS7pBx2nG%2Fj4158tSdMSWJgPPsYsdXyi5lXDA3rDe4ylWXS%2F48cVHTxPKqnp%2BlOIkwFIWOdRHc9q6JKKDRrj%2B8DdDYMfwBU%2BV5p" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Men walk on Moon"
	},
	{
		"id" : 3,
		"title" : "Nixon Resigns"
	},
	{
		"id" : 4,
		"title" : "The Berlin Wall falls"
	}
] )
news.setRow( 3, {
	"id" : 5,
	"title" : "The internet is born"
} )
writeDump( news )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljz0LwjAQhufkVxyZLGTxaxHcnAvqKA5BT5Ni0ppeLUX6370WhdZO98FzD%2FcSlrSvMDawhWdXU6xnoNxVB%2BNRaW4D4R2jfpl4sSby6gRSvKUQTCnYwFx3fY%2FzpDIM7oZRSdHqEbcYcyYzzRRa%2FslyG6bQagyV3pHtKHmGRPYpjkiHnIPQL55mM7Dia1izYfDKo7BGyZav6%2BgId5UvBreQfABU400R" target="_blank">Run Example</a>

```java
testQuery = queryNew( "id,name", "integer,varchar", [ 
	{
		"id" : 1,
		"name" : "jenifer"
	},
	{
		"id" : 2,
		"name" : "ajay"
	},
	{
		"id" : 3,
		"name" : "john"
	},
	{
		"id" : 4,
		"name" : "smith"
	}
] )
querySetRow( testQuery, 3, {
	"id" : 5,
	"name" : "alpha"
} )
writeDump( testQuery )

```

### Overwrite a row using an array

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
querySetRow( q, 2, [ 5, "Cy" ] )
writeOutput( arrayToList( queryColumnData( q, "name" ) ) )

```

Result: Ann,Cy

### Add a row by omitting the row number

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
querySetRow( query: q, rowData: [ 9, "Dee" ] )
writeOutput( queryRecordCount( q ) & ": " & arrayToList( queryColumnData( q, "name" ) ) )

```

Result: 3: Ann,Bob,Dee
