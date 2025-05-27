### Create a query with 4 rows and return a new query containing the 2nd and 3rd rows of the first



<a href="https://try.boxlang.io/?code=eJxNjkELgzAMhc%2Ftrwjx4qCXud3Ew8DLdhiM7SYeimaz0MqIdeK%2FX6sbCCGBL3nvpdVeQwEVSFFJIfYqNLxoRwNKUauVZgs92SdTu8GHFTszdHqDjwt%2BELOJVNa5dPNtJJ5D0DKvNKWA51L1IQgVoOk9vYjVR3PTaQ6ojX%2Ftcsk0jNb%2FhXdrGkrhZ6cgCxWvJjaeytG9UwgWBSarLMG4%2FAJsLDsW" target="_blank">Run Example</a>

```java
data = [ 
	[
		1,
		"James"
	],
	[
		2,
		"Alfred"
	],
	[
		3,
		"Amisha"
	],
	[
		4,
		"Terri"
	]
];
myQuery = QueryNew( "ID,name", "integer,varchar", data );
result = QuerySlice( myQuery, 2, 2 );
writeDump( var="#result#" );

```


### Using a member function



<a href="https://try.boxlang.io/?code=eJxNzsEKgzAMBuBz%2BxQhXhyUwdxu4kHwsh0Gg93EQ9FsFqyMtE58%2B7W6g5cEvvAn6bTXUEANUtRSiJMKBW%2FakkMpGrVptmo5vJi6HZ83tsb1eseXlZ%2FEbKLKJpd2eUzESzi09jvNKeC1UmM4hArQjJ7exOqrue01B%2BriX4dcMrlp8CH4X3F0g2kphUxBFuczG0%2FVZD8phHCByRZIMA5%2FAcQ5Ag%3D%3D" target="_blank">Run Example</a>

```java
data = [ 
	[
		1,
		"James"
	],
	[
		2,
		"Alfred"
	],
	[
		3,
		"Amisha"
	],
	[
		4,
		"Terri"
	]
];
myQuery = QueryNew( "ID,name", "integer,varchar", data );
result = myQuery.slice( 2, 2 );
writeDump( var="#result#" );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kk1Lw0AURdeZX3GZVVomJlFa00pWFhcuFCmuShfTZqyBfHWSKIX%2BeN9kbMGSFhc63DBf572QQypVVplCjJdW6d2T%2BnTBC5krkZQrITeKC%2FAPqdfvUotENkqkRUNnCzBnwRyHz9s65YJW91rR9YweF%2BHkNhAIKRiYu4A5S%2FFd8KrrHn4yOsc%2FaJX0FIzPvuAxzXv4KDrl2RKDOzZr88oFfWFcdSYEMrlSWcztFh5KnW7SQmbYGkPcFPlDPLdN1TZThiEFexhnNJE1nI49yCP2hvN%2BMWw%2F45UmY9ILQgqCYNqFTgPT1XKkEx03GV3kjMaOG1%2FuR%2FZsvyg6z1F85vuoJJnZsK3ezbN0bf4ia%2B2qNlsXNwLXxldylHxAj5qPtR7edJlDq3WpE1tp13WnHD%2Bc%2F4PwPxXkfwFptMt%2F" target="_blank">Run Example</a>

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
// paging
qrySlice = people.slice( 3, 2 );
dump( var=qrySlice, label="qrySlice - from record 3, 2 records" );
 /* Output:
 * | name | dob                 | age |
 * ------------------------------------
 * | Fred | 1960-01-01 00:00:00 | 0   |
 * | Jim  | 1988-01-01 00:00:00 | 0   |
 */
```


