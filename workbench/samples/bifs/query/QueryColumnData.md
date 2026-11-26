### Create and populate a query and output the 'name' column data as an array



<a href="https://try.boxlang.io/?code=eJxljbEKAjEQRGvzFctek4P7hSvEE7QRxFIsVhM4IYm6lyjx690kpdXMDjtvfD4myxlGqHqwHw2I0KtA3i4Sn9UK98Ew4SBu6%2B5futo412tHzLm6U4nURb0KZG3M5uGSDxp8ww%2BAhYeib%2BLbTCy2LfSK7ZJclKlabs2JIv215XdK%2FqlBGCN2rddJ%2FAM2lT0a" target="_blank">Run Example</a>

```java
myQuery = QueryNew( "" )
names = [
	"Indra",
	"Elizabeth",
	"Harry",
	"Seth"
]
queryAddColumn( myQuery, "name", "varchar", names )
result = queryColumnData( myQuery, "name" )
Dump( var="#result#" )

```


### Using a member function



<a href="https://try.boxlang.io/?code=eJwtjcEKwjAQRM%2FmK5btJYXiH%2FQgVtCLIB7Fw9oEKiStbhOlfr2bpKedHebN%2BOUSLS%2FQQr5n%2B9WACLUaydtZ7Jva4Gk0TNiIOrjnjx42DPk7EvOS1TVZ6q7eqWRnzH5y0Y8afKlvAFMfyv0Q9wOxyLJQK7ZzdEGm1vC2z3BHgfTKSaqL%2FqVB6BarQlRi%2FwG9djrm" target="_blank">Run Example</a>

```java
myQuery = QueryNew( "" )
names = [
	"Indra",
	"Elizabeth",
	"Harry",
	"Seth"
]
queryAddColumn( myQuery, "name", "varchar", names )
result = myQuery.columnData( "name" )
Dump( var="#result#" )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVANN%2BqeUaCkpKCppceYm5qcVA4WguTqUKIFDSATIqgUCJK5arEKTWMSXFOT%2BnNDdPQyEXYoqOghJImxKQLkssSs5ILAIyIQZpchWlFpfmlABNBGuG6HRJLEnE0A1UW16UWZLqUppboKEA1aYJANDrNtM%3D" target="_blank">Run Example</a>

```java
myQuery = QueryNew( "" )
names = [
	"xxxx",
	"yyyy"
]
queryAddColumn( myQuery, "name", "varchar", names )
result = queryColumnData( myQuery, "name" )
writeDump( result )

```

### Get the values of a column

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
writeOutput( arrayToList( queryColumnData( q, "name" ) ) )

```

Result: Ann,Bob

### Member function

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
writeOutput( arrayToList( q.columnData( "id" ) ) )

```

Result: 1,2

### Column does not exist

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
try {
	queryColumnData( q, "missing" )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Column missing does not exist in the query.
