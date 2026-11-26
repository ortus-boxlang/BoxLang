### All values greater than 50

Find out if every value in the query is greater than 50


```java
<bx:script>
	data = query( foo=[
		51,
		52,
		535
	] )
	allGT50 = queryEvery( ( Any row ) => {
		return row.FOO > 50
	} )
</bx:script>

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzFkkFPg0AQhc%2Fsr3jhYMBs7WKstkaaqMSDhxpjPJketjJVEgp0CzaN9b87C9qoaZvqxc2Q3WXfDMy3r6C8SAkhbisyiwHNPbiZnpCM85HUT%2BRKuC%2FaPD5rI2Ndkkyykt89QDgPwnHcu2qWuJJXl4b4OOLHQ9A7URIBB3x7poQzlB8J92a2Rt%2FrbNJfGYrXJBxv%2FMB1Mlmj73Z%2F6sUQvoiqSeGBGwyLGoREqkeUhm6zRQu5SZ6STKeYWkAu57T3cVOVRVWeCuxzYAlLjCdmhp9jCaaIpdW1dhhNPUuVJ8uxpQIOKHVaB79VtmqjY5iodb3OVp2FWOuOt9djdk29bnezri3abbAXNHNLE14kecagbJsjKudEGUt1FiM4VKKWsL0angf0whA9eDjPFjD5XH4uBtVkRKbZTs0isuV9hH28CsdQWZkMnmcvM0rGY%2Fboggf7kDMPopsLiUE%2B93zO6EP52NvDLtqzEIFSvi%2Fe%2BFbjlRPqX14ZoWlgQ3vf7YDSVLQTn6OmQuc%2F%2BBz9ClDnL3xW7Vk%2B%2BAJorNOZJfQOoiEvNw%3D%3D" target="_blank">Run Example</a>

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
] )
Dump( var=people, label="people - original query" )
/* Output:
 *
 * | name | dob                 | age |
 * ------------------------------------
 * | Susi | 1970-01-01 00:00:00 | 0   |
 * | Urs  | 1995-01-01 00:00:00 | 0   |
 * | Fred | 1960-01-01 00:00:00 | 0   |
 * | Jim  | 1988-01-01 00:00:00 | 0   |
 */
// data validation - age between 0 and 120
valid = people.every( ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 0) && (DateDiff( "yyyy", row.DOB, Now() ) <= 100))
} )
dump( var=valid, label="valid - age between 0 and 120" )
/* Output: true */
// data validation - age between 40 and 50
valid = people.every( ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 40) && (DateDiff( "yyyy", row.DOB, Now() ) <= 50))
} )
dump( var=valid, label="valid - age between 40 and 50" )
 /* Output: false */
```

### Every row passes the test

```java
q = queryNew( "n", "integer", [ [ 51 ], [ 52 ], [ 60 ] ] )
writeOutput( queryEvery( q, ( row ) -> row.n > 50 ) )

```

Result: true

### Member function where one row fails

```java
q = queryNew( "n", "integer", [ [ 51 ], [ 52 ], [ 60 ] ] )
writeOutput( q.every( ( row ) -> row.n > 55 ) )

```

Result: false
