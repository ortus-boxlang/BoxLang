### All values greater than 50

Find out if every value in the query is greater than 50


```java
<bx:script>
	data = query( foo=[
		51,
		52,
		535
	] );
	allGT50 = queryEvery( ( Any row ) => {
		return row.FOO > 50;
	} );
</bx:script>

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzFkkFPg0AQhc%2Fsr3jhYMBs7WLa2qqYqMSDhxpjPDUetjJVEgq4BZvG%2Bt%2BdBW1M05rqxc2Q3WXfDMy3r6C8SAkhbisyiyHNPbiZnpKM87HUT%2BRKuK%2FaPD5rI2Ndkkyykt%2BNIJyRcBz3rpolruTVpSE%2BjvjxEAyOlETAAd%2BeKeE8yM%2BEezPboB90t%2BmvDMUbEnpbP3CdTDfo%2B%2F11vXiAfyKialp44A7DoiYhkeoxpaHbbNFCbpKnJNMpXiwh1ya193FTlUVVHgvsc2AJy4wnpob1sQRzxNLqWjuMpp7lypMl2VIBB5Q6roPfKlu10TFO1LpB90edxVjrej%2FXY3pNvX5%2Fu64t2m2wGzSDSxNeJHnGpGybYyrnRBlLdRYjOFSilrDBGqAH9MoUPXg4zxYw%2BVx%2BLYbVdEym2b6YRWTL%2BwjP8CYcQ2VlMnievc4omUzYpQse7ETOPIhuLiSG%2BdzzOeMMysfeHnbRnoYIlPL5St%2FtvcYrM9Q%2FvfJC08KWBtccgdJUtBOiTlOi%2Bx%2BIOr9i1P0bolWDNSJ8YzTR6cxC%2BgCECzEP" target="_blank">Run Example</a>

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
 */
// data validation - age between 0 and 120
valid = people.every( ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 0) && (DateDiff( "yyyy", row.DOB, Now() ) <= 100));
} );
dump( var=valid, label="valid - age between 0 and 120" );
/* Output: true */
// data validation - age between 40 and 50
valid = people.every( ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 40) && (DateDiff( "yyyy", row.DOB, Now() ) <= 50));
} );
dump( var=valid, label="valid - age between 40 and 50" );
 /* Output: false */
```


