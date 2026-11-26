### Retrieves query metadata using queryGetResult



<a href="https://try.boxlang.io/?code=eJxtkMsKwjAQRdfJVwxZWcnGx0p0I4o7UXEj4iLWqS20tY6JVaT%2F7qQ%2BoCghYXI53HASkUssjODskO5zLFugkoPOTYZK85hbPCLpq6EwNsTRFqR4SCGYUjCAjvZzjfNNmaJIUUlR6QbVbVJ7k%2FP6xXpN7EQmP%2F5p6zexAk0Ye0ruIJDZfelNPkbTG4bOIltdMMXQQhsiOmV8eO0yRkJIDjCEPrs9Kt5STMbrzWLqq%2BsKJau6d4UXl36%2Faob2FbTg82QgS0osTlxW%2BPDNB0%2B%2BpWLF" target="_blank">Run Example</a>

```java
fruit = queryNew( "id,name", "integer,varchar", [ 
	{
		"id" : 1,
		"name" : "apple"
	},
	{
		"id" : 2,
		"name" : "banana"
	},
	{
		"id" : 3,
		"name" : "orange"
	},
	{
		"id" : 4,
		"name" : "peach"
	}
] )
myQuery = queryExecute( "select * from fruit where id < 4", {}, {
	DBTYPE : "query"
} )
myResult = queryGetResult( myQuery )
writeDump( myResult )

```


### Retrieves query metadata using getResult member function



<a href="https://try.boxlang.io/?code=eJxtkMuqwjAQhtfJUwxZtRIELyvxbA66FRU3Ii5inV6grXVMrCJ9dyc9R6EoYYbJ8PGHLzG5zMIPnB3SfYF1ACo76tIUqDSPpcUESV8NRakhXu1AiocUgikFExhoP7c435SpqhyVFI3uUMMudTAln09s1MVOZMrkS9q4i1VootRTcg%2BhLO4rb%2FIymt8wchbZ6oI5RhZ6ENOp4Oa16xQJITvCFMbs9mi4pJj9brbLuY9uI5Rs2tw1Xlzuv%2Br%2FiX6C9m8XhLKmzOLMFVUAbzJ8AnBfYJ0%3D" target="_blank">Run Example</a>

```java
fruit = queryNew( "id,name", "integer,varchar", [ 
	{
		"id" : 1,
		"name" : "apple"
	},
	{
		"id" : 2,
		"name" : "banana"
	},
	{
		"id" : 3,
		"name" : "orange"
	},
	{
		"id" : 4,
		"name" : "peach"
	}
] )
myQuery = queryExecute( "select * from fruit where id < 4", {}, {
	DBTYPE : "query"
} )
myResult = myQuery.getResult()
writeDump( myResult )

```

### Read the record count and column list

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
meta = queryGetResult( q )
writeOutput( meta.recordCount & " rows, columns: " & meta.columnList )

```

Result: 2 rows, columns: id,name

### Member function

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
writeOutput( q.getResult().type )

```

Result: Query
