### Iterate over query rows instead of bx:loop()


```java
<bx:script>
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

	function newsRow( Any row ) {
		writeOutput( "<tr>" );
		writeOutput( "<td>#row.ID#</td>" );
		writeOutput( "<td>#row.TITLE#</td>" );
		writeOutput( "</tr>" );
	}
</bx:script>

<table>
    <bx:script>
	queryEach( news, newsRow );
</bx:script>

</table>
```

Result: 1 Dewey defeats Truman
2 Man walks on Moon

### Iterate over query rows instead of bx:loop()

```java
<bx:script>
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

	function newsRow( Any row ) {
		writeOutput( "<tr>" );
		writeOutput( "<td>#row.ID#</td>" );
		writeOutput( "<td>#row.TITLE#</td>" );
		writeOutput( "</tr>" );
	}
</bx:script>

<table>
    <bx:output>#queryEach( news, newsRow )#</bx:output>
</table>
```

Result: 1 Dewey defeats Truman
2 Man walks on Moon

### Additional Examples


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
Dump( var=people, label="people - original query" );
/* Output:
 *
 * | name | dob                 | age |
 * ------------------------------------
 * | Susi | 1970-01-01 00:00:00 | 0   |
 * | Urs  | 1995-01-01 00:00:00 | 0   |
 * | Fred | 1960-01-01 00:00:00 | 0   |
 * | Jim  | 1988-01-01 00:00:00 | 0   |
 *
 */
people.each( ( Any row, Any rowNumber, Any recordset ) => {
	recordset.AGE[ rowNumber ] = DateDiff( "yyyy", row.DOB, Now() );
} );
Dump( var=people, label="people - with calculated age" );
 /* Output:
 *
 * | name | dob                 | age |
 * ------------------------------------
 * | Susi | 1970-01-01 00:00:00 | 45  |
 * | Urs  | 1995-01-01 00:00:00 | 20  |
 * | Fred | 1960-01-01 00:00:00 | 55  |
 * | Jim  | 1988-01-01 00:00:00 | 27  |
 *
 */
```


