### Create a query with 4 rows and return a new query containing the 2nd and 3rd rows of the first



<a href="https://try.boxlang.io/?code=eJxNzr0Kg0AMAOD57ilCXBRuqe3qUHCxQ6G0mzgcmtYDT0o8K759o7YghAQ%2B8tfYYCGDErQqtVIHIwkv1tOAWlVm03TVc%2FdkanZ83Ni7obU7Pq38IGa3qK60n28j8Sx31nqlKQYsctPLHTSArg%2F0IjYfy3VrWahZ3ko00zB24T9371xNMfy2GUglpGliFygf%2FTsGWZBhtE1FCMkXWaA6IA%3D%3D" target="_blank">Run Example</a>

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
]
myQuery = QueryNew( "ID,name", "integer,varchar", data )
result = QuerySlice( myQuery, 2, 2 )
writeDump( var="#result#" )

```


### Using a member function



<a href="https://try.boxlang.io/?code=eJxNzr0KgzAQAOA5eYrjXBRCobarQ8GlHQqFbuIQ9FoDRsolVnz7ntrB5Q6%2B%2B21ttFBABVpVWqmjkYA36ymgVrXZNF%2F10r%2BY2h2fNvYudHbH55WfxOwW1bX282MknuXOmu80pYDX0gxyBw2gGyK9ic3XctNZFmqXtzLNFMY%2Bytx%2FwyH0rqEUcgO5lCd2kcrRf1KQ0QKTrT9ByH74KTgM" target="_blank">Run Example</a>

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
]
myQuery = QueryNew( "ID,name", "integer,varchar", data )
result = myQuery.slice( 2, 2 )
writeDump( var="#result#" )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kk1Lw0AURdeZX3GZVVomJlVa00JWFhcuFCmuShfTZKyBfHWSKIX8eN9kNGBpiwsdbpiv817IIZUqq0whwnOr9OFRfbjghcyVSMqtkDvFBfi71PGb1CKRjRJp0dDZGsxZM8fhq7ZOuaDVnVZ0vaTHxWR%2BGwhMKBiZu4A5G%2FFV8KLrE%2Fx8eo6%2F1yo5UTA7%2B4KHND%2FBh%2BExzzYYsWWbVy7oA6OqFyGQya3KIm638FDqdJcWMsPeCOJU44%2Fx1DZV2ywYxhR0MMZoImc4Hh3IIjrDeb8Ytp%2BxSpPx6AUTCoJg0YdOA9PVciQTPTefXuSMxJ6bXe5H7my%2FMDzPUXzm%2B6gkidmxvT6ssjQ2%2F5CVdlWbrYsbgWvSlQyKv8lB8lDq4VWXObSKS53YQruujXD8MP4Puv9Uj%2F8JRLTKkw%3D%3D" target="_blank">Run Example</a>

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
 *
 */
// paging
qrySlice = people.slice( 3, 2 )
dump( var=qrySlice, label="qrySlice - from record 3, 2 records" )
 /* Output:
 * | name | dob                 | age |
 * ------------------------------------
 * | Fred | 1960-01-01 00:00:00 | 0   |
 * | Jim  | 1988-01-01 00:00:00 | 0   |
 */
```

### Take two rows starting at row 2

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ], [ 4 ] ] )
result = querySlice( q, 2, 2 )
writeOutput( arrayToList( queryColumnData( result, "id" ) ) )

```

Result: 2,3

### Member function with no length returns the remaining rows

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ], [ 4 ] ] )
writeOutput( arrayToList( q.slice( 3 ).columnData( "id" ) ) )

```

Result: 3,4

### Offset outside the query

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ] ] )
try {
	querySlice( q, 9 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Offset is outside the query row range.
