### Builds a simple query using queryNew and queryAddRow

CF10+ Pass in row data directly to queryAddRow argument.

<a href="https://try.boxlang.io/?code=eJwljLEKAjEQRGvzFctWCtvYKhbC1RbiDwQzasCLurcxHOK%2Fu%2Bd1M8N7U9AG2tGrQscD2pI4J7Fsd7B4LoYrVN5Rz7eoTKtt%2BKP7lI4Pp4vrQp%2BwcI1pQ2vxOOveuEPDSAkXRBvopLWPhcN3ummaDV3tn%2FPJNP0AXTYr0A%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" );
queryAddRow( news, {
	"id" : 1,
	"title" : "Dewey defeats Truman"
} );
writeDump( news );

```

Result: 

### Builds a simple query using queryNew and queryAddRow member syntax

CF10+ Pass in row data directly to queryAddRow argument.

<a href="https://try.boxlang.io/?code=eJwljMEKwjAQRM%2FmK5Y9VQiCV8Vbzx6kPxCaUQM26nZjKMV%2Fd4O3mWHey6gznehdIMsZtSNO0WvSB9hbzoobxH%2BCjPcgTNujy0bsQoyXp71XtzGA6UB7b%2FEPWuMeFQtFXBF0pkHKFDK7bxNUSYq%2BTK%2BOmqtNPzgHKZw%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" );
news.addRow( {
	"id" : 1,
	"title" : "Dewey defeats Truman"
} );
writeDump( news );

```

Result: 

### Builds a simple query using queryNew queryAddRow and querySetCell

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

Result: 

### Builds a simple query using queryNew queryAddRow and querySetCell with rows number

The example above could be simplified this way:

<a href="https://try.boxlang.io/?code=eJx9jjEOwjAMRXdOYWUCKUu7IgZE1zIAF4jIByJSB9KEqLcnpWo2uliW%2Fd%2BzGamnHb0j%2FHBEWpMwWgYTLITMPQfc4eVH%2BetDeUGb7eoX3Wt9cjnNGZdUl%2FkZ4QBr50WWjZoql%2BpvplxrkDCQxg0q9HTxsVO8SE72Wix9UOwtmJKyT3JMrXM8U8mbgCZ2rwkZR1%2BrXFX%2B" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" );
queryAddRow( news, 2 );
querySetCell( news, "id", "1", 1 );
querySetCell( news, "title", "Dewey defeats Truman", 1 );
querySetCell( news, "id", "2", 2 );
querySetCell( news, "title", "Men walk on Moon", 2 );
writeDump( news );

```

Result: 

### Builds a simple query using queryNew queryAddRow with multiple rows as an array

CF10+ The example above could be simplified even more this way:

<a href="https://try.boxlang.io/?code=eJxdjrEOwjAMROfkK6xMIHmBEcSA1LUMiA0xRMRAReuAmxBViH8naSfYzqd7vmNKPWzgGUmGHaUZmMZhaEJLBrPmQFcSfFk536wYmK%2F1GN06t%2Fc5zRlHOGr11kpl1MAKFlj09COfpqJEAzi6kA09HCR2lo1WH%2Fyhln9UTQzJtnfwDLX3I6FPZUCSJlAVu8dUX6wvBTs49A%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" );
queryAddRow( news, [
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Men walk on Moon"
	}
] );
writeDump( news );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1jjELwjAUhOfmVxyZKhRKqaAoDgUdHCzoKg7BBAw0qb4mliL%2Bd9OK0KFO7w7u3nemO3pFHTZ49LdUbQyuZWKFURyzNRNSKnmq2%2BYXKWRvY5hvM0EeYkhTtLqqQMp5ssjZdPbMomyRsIgPb%2FHUAoJIdLC1E07XlrPLQA3%2BpiiU%2F2AnmPM%2FzBeL9luskC0DuCwOu6BH%2FMaRv7rRgHc%2FQHpzH8PYBygtXwc%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "id,name" );
addedRows = queryAddRow( myQuery, 3 ); // will return 3
queryAddRow( myQuery, [
	17,
	"added via array notation"
] );
anotherRow = queryAddRow( myQuery ); // will return 4
queryAddRow( myQuery, {
	ID : 18,
	NAME : "added via struct notation"
} );
dump( myQuery );

```


