### The simple queryGetRow example

Here we've a example to get the particular row value from myquery.


```java
<bx:set myQuery = queryNew( "id,name,age", "integer,varchar,integer", [ 
	[
		1,
		"Dharshini",
		20
		],
	[
		2,
		"Subash",
		18
		]
	] ) >
<bx:dump var="#queryrowdata( myQuery, 2 )#"/>
```

Result: 

### The simple getRow example

We've example to get the particular row value from myquery using script syntax.


```java
<bx:script>
	var myQuery = queryNew( "id,title,author", "integer,varchar,varchar", [
		[
			1,
			"Charlottes Web",
			"E.B. White"
		],
		[
			3,
			"The Outsiders",
			"S.E. Hinton"
		],
		[
			4,
			"Mieko and the Fifth Treasure",
			"Eleanor Coerr"
		]
	] );
	writeDump( myQuery.getRow( 3 ) );
</bx:script>

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVdIBMvNKdMoSi5IzEouUFDStuQr1ElNSgvKBqkwg3OLUEufUnBywLqAGQyDCKmEERFgljIEIq4SJDqYdMGfl54EoDJtg0iXl%2BUpY7INLZxSlghgY9sIUpOWXFilBrU8pzS3QUChEMANBAQUMApfEkkSgBNgekCwAYFhZSg%3D%3D" target="_blank">Run Example</a>

```java
q = queryNew( "id,name", "int,varchar" );
q.addRow( 4 );
q.setCell( "id", 1, 1 );
q.setCell( "id", 2, 2 );
q.setCell( "id", 3, 3 );
q.setCell( "id", 4, 4 );
q.setCell( "name", "one", 1 );
q.setCell( "name", "two", 2 );
q.setCell( "name", "three", 3 );
q.setCell( "name", "four", 4 );
dump( q );
dump( QueryRowData( q, 2 ) );

```


