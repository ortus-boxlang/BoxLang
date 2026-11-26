### Create and populate a query and output the 'name' column data as an array



<a href="https://try.boxlang.io/?code=eJxljcEKwjAQRM%2FmK5btJUL%2FoPQgVtCLIB7Fw2oCFZJWt4kSv95NcvQ0s8POPJ9O0XKCHooe7UcDIqw7NZG3i%2BQXtcLDZJiwFbdzjy%2FdbBjLtSfmVNw5R%2BraqVee2RiznV30kwZfAS1gHkTRN%2FF9JBZbEcJiu0QXBFbatTpQoL96fh6if2qQlR6bWmxK%2FgOIdT5L" target="_blank">Run Example</a>

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



<a href="https://try.boxlang.io/?code=eJwtjcEKwjAQRM%2FmK5btJULxB0oPYgW9COJRPKxNoEISddso9evdJD3t7DBvxs%2FnaHmGFvI92a8GRFg3KpC3o%2FhXtcJjMExYi9q7x4%2FudhrydyDmOatLstStUe9UszVm93TRBw2%2BDNSAqRDlfoj7gVhkmZAttmN0k4wt6U2f6Y4m0guYYl30Lw3Ct1gVpMr%2BHwSjPBc%3D" target="_blank">Run Example</a>

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

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVANN%2BqeUaCkpKCprWXHmJuanFQPFoLk6lCiBQ0gEyKoFAiSvWmqsQpNoxJcU5P6c0N09DIRdijo6CEkifEpAuSyxKzkgsAjIhJgGNLEotLs0pAZoJ1g3R6pJYkoihHaS4vCizJNWlNLdAQwGqDygIAPqQOAQ%3D" target="_blank">Run Example</a>

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
