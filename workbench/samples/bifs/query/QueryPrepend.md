### Builds a simple query using queryNew and queryPrepend

Add new query to the end of the current query using queryPrepend.

<a href="https://try.boxlang.io/?code=eJydzTELwjAQBeA59yuOTA1ksIqDgpuTg7iXIsEeNmCv8WwN%2Fntj0A7i5PrevfvYdXTDDV5HkseeYoGaunA5%2BsZyqrRF7XmgM4m9Ozm1TlJUIagKlPqcalxjOV%2Bs7CvLsxToXd%2ByBlVDjQaYIv9F%2FZCWs%2FJLckyTlN8fhAJxU2BGLU68gSh%2BoO3YhXeJ5gky%2BUz3" target="_blank">Run Example</a>

```java
names = queryNew( "empl_id,name", "integer,varchar", [ 
	[
		"empl_id" : 1239,
		"name" : "John"
	]
] )
newnames = queryNew( "empl_id,name", "integer,varchar", [
	[
		"empl_id" : 1501,
		"name" : "Jane"
	]
] )
queryPrepend( names, newnames )
writeDump( names )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJydjrEKwjAQhufkKY6bWuhQipPgUFAEwaKok3QI9bAZEmuaWER8d6%2BVYmenO77%2F%2BL%2Fz1Pp9IPeEBdz7WVAXAVplCBJQV8IE8KFcVSvHwAZDTlcMXyBFkW9XMIezFAIPodWY9NvJtShFyXu%2BHuMs7aNsxly%2BIZaWuuO%2FZu77Go329aDM0q9vwJtbbfFnK9k21O8cNWQvEfhRzKXTN2LZOe1pGUwzOYL4A0YkUSg%3D" target="_blank">Run Example</a>

```java
testQuery = queryNew( "name , age", "varchar , numeric", { 
	NAME : [
		"Susi",
		"Urs"
	],
	AGE : [
		20,
		24
	]
} )
newTestQuery = queryNew( "name , age", "varchar , numeric", [
	[
		"Smith",
		20
	],
	[
		"John",
		24
	]
] )
queryPrepend( testQuery, newTestQuery )
writeDump( testQuery )

```

### The second query supplies the leading rows

```java
a = queryNew( "name", "varchar", [ [ "A1" ] ] )
b = queryNew( "name", "varchar", [ [ "B1" ] ] )
result = queryPrepend( a, b )
writeOutput( arrayToList( queryColumnData( result, "name" ) ) )

```

Result: B1,A1

### Only the second query is modified

```java
a = queryNew( "name", "varchar", [ [ "A1" ] ] )
b = queryNew( "name", "varchar", [ [ "B1" ] ] )
a.prepend( b )
writeOutput( queryRecordCount( a ) & "," & queryRecordCount( b ) )

```

Result: 1,2
