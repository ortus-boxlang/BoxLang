### Register a scalar function and use it in a Query of Queries

The function is registered once and can then be called by name from SQL that runs against in-memory queries (`dbtype: "query"`).

```java
queryRegisterFunction( "doubleIt", ( value ) -> value * 2 )
q = queryNew( "n", "integer", [ [ 5 ], [ 10 ] ] )
result = queryExecute( "SELECT doubleIt( n ) AS doubled FROM q", {}, { dbtype: "query" } )
writeOutput( result.doubled[ 1 ] )

```

Result: 10

### Register a function that works on strings

```java
queryRegisterFunction( "shout", ( text ) -> text.ucase() )
q = queryNew( "name", "varchar", [ [ "ann" ] ] )
result = queryExecute( "SELECT shout( name ) AS loud FROM q", {}, { dbtype: "query" } )
writeOutput( result.loud[ 1 ] )

```

Result: ANN
