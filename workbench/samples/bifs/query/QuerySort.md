### Sort a query using the querySort function on a column with date values in ascending order



<a href="https://try.boxlang.io/?code=eJytUU1Lw0AQPWd%2FxaOnXVhN68dFqBC7PXgRrN6kh9UdSqDZxOmGWKT%2F3UmKEM%2Fm8mbeY%2Ba9gclzvLflPsDjsyU%2BKvryVbOn555geRafqNOYlcF%2BMPlEwQnMrCgx0Y7YprKiQ5I9Ed9U9q2y7NHhDgsr3WqzLl7XzgmIFGS1CEHsggyzj2Hj4440LhbzuYUAjEWsO21gVHayI7urae2up7W7mdbu9v92aislz3GoOaFMavjlixCN8ZctNIp4BNddYX%2B7Bxgs7yHnMKWW4xC%2BqqvGswT2o5ej4%2BywMlYk%2BXROD23V9Okdl4mckL%2FpMD90xaxM" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,createdDate", "integer,timestamp", [
	{
		ID : 1,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 2,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 3,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 4,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 5,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	}
] )
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return dateCompare( rowA.CREATEDDATE, rowB.CREATEDDATE )
} )
// dump it
writeDump( exampleQuery )

```


### Sort a query using a sort member function on a column with date values in descending order



<a href="https://try.boxlang.io/?code=eJytUstqwzAQPFtfMeQkgRInfVwKCThRDr0UGnorPajVEgSx7Coybij5964aCs65vox2h52ZhVVZ4r3zBweLz47iSdCXrdsDPecGywv5RL3ExDv9EckmcoZhopkJifYUdfI1HRPrmHwVxbcoikeDByw0V5vdtnrZGsPAlGNp5RzbOR6ONridDXuSmC7mcw0GKI3Q9FJBieKsB3Y349rdjmt3N67d%2Ff%2FtxBs%2FZYljExN8ujrtLJMSElU4ITZ9pf%2BqNRSWK%2FAikVIXA6YyB2%2BaurWRw%2FLwbLCY%2FhUNGSglzpdo19Vtju6jT2S4kbj6YOoHIoaqlg%3D%3D" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,createdDate", "integer,timestamp", [
	{
		ID : 1,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 2,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 3,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 4,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 5,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	}
] )
// sort it
exampleQuery.sort( ( Any rowA, Any rowB ) => {
	return -(dateCompare( rowA.CREATEDDATE, rowB.CREATEDDATE ))
} )
// dump it
writeDump( exampleQuery )

```


### Sort a query using a sort member function on a column with string values in ascending order



<a href="https://try.boxlang.io/?code=eJxtkU1rwkAQQM%2FZXzHktIHF0NpeBAU1PeSi%2FbqJh0120EWzm042TW3xv3ejCDHkNvN48GAmjiGr9VGBhK8a6cTwRxblEd%2FaBaZXuMKGQ6iVUFjlpEunrQmFJ8bhDkl8S8r3kjzasOCPBUGawAQehJ%2BSl4%2Fle%2Fr6ma5XHoXK7kIWnEVHexzQcun62nhAyzSpvvc04P1iRrIvPg%2BIlZEHbEW2hYjFMVSWHGh3d5VRCzlwmJsTkG3m4jYtIILpDHyC0NVkILdFKQn5RRt1YuKid4nvna9NVRdl22xIO0z8wuHuKdE%2FEkp3gw%3D%3D" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 1,
		DESCRIPTION : "dog"
	},
	{
		ID : 2,
		DESCRIPTION : "cat"
	},
	{
		ID : 3,
		DESCRIPTION : "bird"
	},
	{
		ID : 4,
		DESCRIPTION : "zebra"
	},
	{
		ID : 5,
		DESCRIPTION : "snake"
	}
] )
// sort it
exampleQuery.sort( ( Any rowA, Any rowB ) => {
	return compare( rowA.DESCRIPTION, rowB.DESCRIPTION )
} )
// dump it
writeDump( exampleQuery )

```


### Sort a query using the querySort function on a column with string values in descending order



<a href="https://try.boxlang.io/?code=eJxtkEtLw0AUhdeZX3HIKoHR4GsjVKjGRTb1UXfFxSRzqYPNJN5MjFX6352kCDFkd87hu3xwkwR5a3YaCh8t8V7QlyrrHT31BYvjuKIuQmi01NQUbGpnKhtKv1hHW2L5qbh4U%2BynjQh%2BRBBkKa5xJn1K79d3z9njS%2Faw8lOoq20ogoMcYeczWKHcFLuYwXLDespdznDflLOaglczYGPVO%2FWgeEUskgRNxQ7GieENa18ijB8kEWFp9%2BCqW8q%2FdIsYixt4EZNr2eIkKqqyVkzRAJ6OpHI4GC%2BIY3E4ynVb1r28Y%2BMo9eW%2FHPEvXf56NQ%3D%3D" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 1,
		DESCRIPTION : "dog"
	},
	{
		ID : 2,
		DESCRIPTION : "cat"
	},
	{
		ID : 3,
		DESCRIPTION : "bird"
	},
	{
		ID : 4,
		DESCRIPTION : "zebra"
	},
	{
		ID : 5,
		DESCRIPTION : "snake"
	}
] )
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return -(compare( rowA.DESCRIPTION, rowB.DESCRIPTION ))
} )
// dump it
writeDump( exampleQuery )

```


### Sort a query using the querySort function on a column with numeric values in ascending order



<a href="https://try.boxlang.io/?code=eJxtkE1Pg0AURdfMr7hhBcko1o%2BNEU0rLthUbd0ZFwMzqRPpgI%2BhWE3%2Fu0OJpiK7d09Oct97UYSs0YWEwHujaMvUh1hXhXrsAuIezlUbwNeSS1XnpCurS%2BNzR4xVK0V8Iyh%2FFeTQM%2FO%2BmOelCS5xxt2U3C1vF%2BnDU3o%2Fd8iX5cpn3o4faBcjWi7sUDsf0TJNcuhNRrxPlZEYiqcjYm3Em%2BpE9oKQRRHqkiy0Zfs3LF0IcPggjgBTswWV7ZT%2FTDOEiK%2FhikjZhgw2ogj2yrErDnH1C2Y9uMHRxLUPtDj%2B7510B7Jdv5ts1lW3W0vaqsSFv7sh%2FAa%2FJIBl" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 3,
		DESCRIPTION : "dog"
	},
	{
		ID : 5,
		DESCRIPTION : "cat"
	},
	{
		ID : 4,
		DESCRIPTION : "bird"
	},
	{
		ID : 1,
		DESCRIPTION : "zebra"
	},
	{
		ID : 2,
		DESCRIPTION : "snake"
	}
] )
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return val( rowA.ID ) < val( rowB.ID ) ? -1 : val( rowA.ID ) == val( rowB.ID ) ? 0 : 1
} )
// dump it
writeDump( exampleQuery )

```


### Additional Examples


```java
people = QueryNew( "name,dob,age", "varchar,date,int", [ 
	[
		"Susi",
		CreateDate( 1970, 1, 1 ),
		70
	],
	[
		"Urs",
		CreateDate( 1995, 1, 1 ),
		40
	],
	[
		"Fred",
		CreateDate( 1960, 1, 1 ),
		50
	],
	[
		"Jim",
		CreateDate( 1988, 1, 1 ),
		30
	]
] )
Dump( var=people, label="people - original query" )
QuerySort( people, "name", "asc" )
dump( var=people, label="people - sorted by name" )
QuerySort( people, ( Any row1, Any row2 ) => {
	return compare( arguments.ROW1.AGE, arguments.ROW2.AGE )
} )
dump( var=people, label="people - sorted by age" )

```

### Sort ascending using a comparator

```java
q = queryNew( "id,name", "integer,varchar", [ [ 3, "Cy" ], [ 1, "Ann" ], [ 2, "Bob" ] ] )
querySort( q, ( a, b ) -> a.id - b.id )
writeOutput( arrayToList( queryColumnData( q, "name" ) ) )

```

Result: Ann,Bob,Cy

### Member function sorting descending

```java
q = queryNew( "id,name", "integer,varchar", [ [ 3, "Cy" ], [ 1, "Ann" ], [ 2, "Bob" ] ] )
q.sort( ( a, b ) -> b.id - a.id )
writeOutput( arrayToList( q.columnData( "name" ) ) )

```

Result: Cy,Bob,Ann
