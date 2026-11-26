### Maps query results

Manipulates query column

<a href="https://try.boxlang.io/?code=eJxlT8FKw0AUPO9%2BxbAHSWAp1KOQW3oomBYh4EE8rO1LXW02dbPrEqT%2F7ttYFPX03gwzb%2BY5SiMqvEXy04ZSAWX3OthwJKV5d4EO5PW78btn45l6gBQfUgiWKdxgqfP%2BpWeoako0YU8dmTCi9bE3Tklx1r9c139cjXFI5vg6YnBohmG2yEeUMnkbqI79qYDLTUspu%2Bh2wbKwN6e7XLuAcRO2Ty8owSG2KzJYrGtUFZZMZtSu29sVP6o2q3tOxNUPK4WnEL3LjDxLzpnPsniejbmE6%2B%2FEf8Uu7CeQFWIl" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] )
writeDump( news )

function mapQuery( any Obj ) {
	if( Obj.ID == 1 ) Obj.TITLE = "NEW: " & Obj.TITLE
	return Obj
}
newQuery = QueryMap( news, mapQuery )
writeDump( newQuery )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1kE1Lw0AQhs%2FZX%2FGypwQHSQRrc6igBg%2BCFRFPIYdtM9WA%2BXCSGIL4352VCiLtMguzO898vR233RtjhceRZV7zFMI2rmYq2w25F7YE%2B%2BFk%2B%2BqESjcwVc2gfzlMkJsgsE9jX1lS70ZYw5neEEl6ERMSNUQ%2BFpugoH3Cs%2FQH%2BPT8GH8rXB5IWBxtcFfVB%2Fjl8j9vCkRGuNfd3%2F3u964L0f3IQQhx1cyQdqJfZz3WG5b9k7etlD0PiLC6xKcJFMhhvV4otKDvmlW7nYo561HBFDjNHq4Jf8c6i5MFQS3VQhFOkGglHkZpPG%2B%2BdMBJKqXHWmfzs0bf7ONv2A%3D%3D" target="_blank">Run Example</a>

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
res = queryMap( people, ( Any row, Any rowNumber, Any recordset ) => {
	row[ "age" ] = DateDiff( "yyyy", row.DOB, CreateDate( 2016, 6, 9 ) ) + 1
	return row
} )
writeDump( res )

```

### Transform each row

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "ann" ], [ 2, "bob" ] ] )
mapped = queryMap( q, ( row ) -> {
	row.name = row.name.ucase()
	return row
} )
writeOutput( arrayToList( queryColumnData( mapped, "name" ) ) )

```

Result: ANN,BOB

### Member function with the row number

```java
q = queryNew( "name", "varchar", [ [ "ann" ], [ "bob" ] ] )
mapped = q.map( ( row, rowNumber ) -> {
	row.name = rowNumber & "-" & row.name
	return row
} )
writeOutput( arrayToList( mapped.columnData( "name" ) ) )

```

Result: 1-ann,2-bob
