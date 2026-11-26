### Filter a query



<a href="https://try.boxlang.io/?code=eJx9kEFLw0AQhc%2FZXzHsKYGloN6ECLFRKESRkouIyLaZtks3u3UzMQTpf3eSaKkInnb2wffezHPYNZDCe4uhf8QuBmkqRf0BFRmyKBULjnCLQX3osN7p0yshESOWVdXSM%2BnYSsGLiD5FFC1yuIYLxVP5%2FHTHs1x5v5ejsCiLUZlb31aQkdWNFNFRnZGX%2F5OFDxX4DZQ7hKVx2z%2F81Tm%2FMbb%2BzT%2BgA%2BPg1ur1fkDFKx8zxJy6uDeWMPwcFUPmengbPpBAegMcFJDa4CZxNkal6feu4sh2XTCEeVsf4slx7m1bu1yTjmGM4mqnjiGZkS9MQ9w%2Bi5yQfAHianJh" target="_blank">Run Example</a>

```java
news = queryNew( "id,type,title", "integer,varchar,varchar" )
queryAddRow( news, [
	{
		ID : 1,
		TYPE : "book",
		TITLE : "Cloud Atlas"
	},
	{
		ID : 2,
		TYPE : "book",
		TITLE : "Lord of The Rings"
	},
	{
		ID : 3,
		TYPE : "film",
		TITLE : "Men in Black"
	}
] )
books = queryFilter( news, ( Any _news ) => {
	return _news.TYPE == "book"
} )
writeDump( queryColumnData( books, "title" ).toList( ", " ) )

```

Result: Cloud Atlas, Lord of The Rings

### Filter a query as member function



<a href="https://try.boxlang.io/?code=eJx9kMFKw0AQhs%2FZpxj2lMASaL0JEWLjoRBFSi4iImszbZdudutmYgjSd3eSiFQETzt88M2%2F8zvsW8jgvcMwPGAfgzS1ouGEigxZlIqBI9xjUB86bA%2F655WQiEnL63rj2XS8SsGziD5FFK0LuIaF4ql6erzjWb55f5QTWFflRFbWdzXkZHUrRXRWF%2Bbyf7P0oQa%2Fg%2BqAsDFu%2F8e%2FuvR3xja%2F%2FXt0YBzcWr09jqp44WPGmLGL8Y6UHcIQQwy5G%2BB1ZJBAdgMcEZC64GaYTiFZ9v1LceZFfTCERdec4rnXlbdd4wpNOoYphEud24UkJV%2Balrh3hpyQfAENXHAt" target="_blank">Run Example</a>

```java
news = queryNew( "id,type,title", "integer,varchar,varchar" )
queryAddRow( news, [
	{
		ID : 1,
		TYPE : "book",
		TITLE : "Cloud Atlas"
	},
	{
		ID : 2,
		TYPE : "book",
		TITLE : "Lord of The Rings"
	},
	{
		ID : 3,
		TYPE : "film",
		TITLE : "Men in Black"
	}
] )
books = news.filter( ( Any _news ) => {
	return _news.TYPE == "book"
} )
writeDump( queryColumnData( books, "title" ).toList( ", " ) )

```

Result: Cloud Atlas, Lord of The Rings

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kk1Lw0AQhs%2FZX%2FGSU1o2TXpoTQsRjEXQQ6uIByk9bM2qgXx1m1iK9b87m9UikoggLhP2Y96Z2X0mpSzKVCLETS3Vfi53DuxcZJLHxdrmsF%2BEengWiseikrRfgllLZln2bb1NbE6rcyXJNaPPwXBy4nMMydDTPp9ZK%2F4RcKe2LfrJqEt%2FoWTcEjDuLHCVZC36IOjSR%2FqBv9CzFXpsVmelA4IRlg0wjlSsZRraZgsXhUqeklyk2GiQNsV4fSzqqqyrKUOfDAdosjQRW3wfB61wfxwmhyZPk2bt%2BkMy%2BP60MZPjAEKNRjEZdSg03EYx7spBNE2OIOhQRPoR3QqPeR4ek7SSiuisC5UjyRs126j9dYMtotPLXJ%2FRD2hIDkyIAwdn%2BR6q2PHPxbzO1lKZLaWgjgn0EJ7ilVlKVjVVuJeCQkk7mC0i7QxNxTdqR3xsYUv9Yz%2Fb7uYi%2BnJ93Vr8U2%2F%2FTv0dIW%2FsyA%3D%3D" target="_blank">Run Example</a>

```java
people = QueryNew( "name,dob", "varchar,date", [ 
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
	],
	[
		"Bob",
		CreateDate( 1988, 1, 1 ),
		0
	]
] )
Dump( var=people, label="people - original query" )
/* Output:
 *
 * | name | dob                 |
 * ------------------------------
 * | Susi | 1970-01-01 00:00:00 |
 * | Urs  | 1995-01-01 00:00:00 |
 * | Fred | 1960-01-01 00:00:00 |
 * | Jim  | 1988-01-01 00:00:00 |
 * | Bob  | 1988-01-01 00:00:00 |
 */
// filter - born in 1988
qryPeopleBornIn1988 = people.filter( ( Any row, Any rowNumber, Any qryData ) => {
	return Year( row.DOB ) == 1988
} )
dump( var=qryPeopleBornIn1988, label="qryPeopleBornIn1988 - Born in 1988" )
 /* Output:
 *
 * | name | dob                 |
 * ------------------------------
 * | Jim  | 1988-01-01 00:00:00 |
 * | Bob  | 1988-01-01 00:00:00 |
 */
```



```java
<bx:script>
	q = QueryNew( "name, description" )
	bx:loop times=3 {
		getFunctionList().each( ( Any f ) => {
			var fd = getFunctionData( arguments.F )
			var r = QueryAddRow( q )
			QuerySetCell( q, "name", fd.NAME, r )
			QuerySetCell( q, "description", fd.DESCRIPTION, r )
		} )
	}
	dump( var=q.RECORDCOUNT, label="demo data set size" )
	s = "the"
</bx:script>


<bx:timer type="outline" label="Query of Query">
	<bx:query dbtype="query" name="q1">
		select 	name, description
		from 	q
		where 	description like <bx:queryparam value="%#s#%" sqltype="varchar">
	</bx:query>
</bx:timer>
<bx:dump var="#q1.RECORDCOUNT#">

<bx:timer type="outline" label="query.filter() with scoped variables">
	<bx:script>
		q2 = q.filter( ( Any row ) => {
			return (arguments.ROW.DESCRIPTION contains s)
		} )
	</bx:script>
	
</bx:timer>
<bx:dump var="#q2.RECORDCOUNT#">

<bx:timer type="outline" label="query.filter() without unscoped variables">
	<bx:script>
		q3 = q.filter( ( Any row ) => {
			return (row.DESCRIPTION contains s)
		} )
	</bx:script>
	
</bx:timer>
<bx:dump var="#q3.RECORDCOUNT#">
```


