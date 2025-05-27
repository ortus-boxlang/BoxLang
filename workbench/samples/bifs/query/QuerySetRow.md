### Builds a simple query using queryNew and querySetRow

Sets third row of query overwriting news entry

<a href="https://try.boxlang.io/?code=eJxtkDFvAjEMhefLr3jK1EpZWtqlVZeKFQZAYkAdUjAQNTitkyOgiv9eH3Th2sWyrfc%2BJ4%2BpZrzgqyU5jqnewIaVK6FEsk57LrQhcXsvy60XXS1gmm%2FTNCqzeMKd6%2FqLXkc7pEpHrGhNvmTMpN15tqY5uSvXfc81Ikb18QOJMUrpH8eg5xiHg2onlMOG81%2F5Q08%2B2xJeSWJgzH2MWGs528wbbp%2FN%2BfdTKpOkAbAm4vQglPeLe1Rcj9YlI0wFIeM9iT751JGqhELDdvd54XSrHzO%2FYVg%3D" target="_blank">Run Example</a>

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
] );
querySetRow( news, 3, {
	"id" : 5,
	"title" : "The internet is born"
} );
writeDump( news );

```

Result: 

### Builds a simple query using queryNew and someQuery.setRow member syntax

Sets third row of query overwriting news entry

<a href="https://try.boxlang.io/?code=eJxtkMFKQzEQRdcvX3HJqkIQtLpR3Ei37aIUuiguop22wXSik7ymRfrvTtSNTzdhMtxzGC5TzXjAe09ymlEdwYa1K6FEsk5nLrQlcQcvLzsvulrBdB%2Bm6zRmcYcr1%2BbvvH7thCqdsKYN%2BZKxkH7v2Zru7H5R1wNqSozq4ysSY5rSP8R4QMzCUbNzymHL%2BW%2F8ZhBf7AiPJDEwlj5GbPT5wswTLu4NawmXmco8aQFjBzX9iG5VNPC0ToSpIGQ8J9Fjz81RJRSa9Pu3EZqurT4ByRxfJA%3D%3D" target="_blank">Run Example</a>

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
] );
news.setRow( 3, {
	"id" : 5,
	"title" : "The internet is born"
} );
writeDump( news );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljz0LwjAQhufkVxyZFLL4tShuzoI6ikPQ06S0aU2vliL9716KQmun%2B%2BC5h3sJSzpUGBrYwjPWPdYTUO6mvclQaW494QODfplwtSbw6gxSvKUQTClYw0zHvsN5Ugl6d8egpGj1gJsPOZOYZgwt%2FmS59WNoOYTKzJGNlLzAdCO7GCekY85J6JdPsxrY8VWsWNH7JS2sUbKN53VwhLsqK3rHcf8BgkBNzA%3D%3D" target="_blank">Run Example</a>

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
] );
querySetRow( testQuery, 3, {
	"id" : 5,
	"name" : "alpha"
} );
writeDump( testQuery );

```


