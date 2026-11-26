### Dump array of query columns



<a href="https://try.boxlang.io/?code=eJxdjjELwjAUhOfkVxyZWsiio%2BAgutbJTRwe9qnF9qWmiaGI%2F93UTrrdHd8HJ5wGrPGI7Mc9pwKmqW1oQsvG5iyBr%2Bztk%2Fz5Rj5PR2j10kplzGCFhZ3yzOdqdpx4RM0XpjDg4GNHYrR62x9r%2BWdVJEjU3gc4QeXcV9EnlLqOXV%2FM77aujZ1svKexgEy3S5QfuLk3Ig%3D%3D" target="_blank">Run Example</a>

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
dump( queryColumnArray( news ) )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUILE0tqvRLLddQUEpMTFRS0ORKLCoyBEpEx3KB5RxTUpzzc0pz8zQUCosqdRSUkpKUdBTAijS5yosyS1JdSnMLgJIgxRCVjkVFiZVg5QqaCpoAuZ8hTQ%3D%3D" target="_blank">Run Example</a>

```java
qry = QueryNew( "aaa" )
arr1 = []
QueryAddColumn( qry, "bb", arr1 )
writeDump( queryColumnArray( qry ) )

```

### Column names as an array

```java
q = queryNew( "id,title", "integer,varchar" )
writeOutput( arrayToList( queryColumnArray( q ), "|" ) )

```

Result: id|title

### Member function

```java
q = queryNew( "id,title", "integer,varchar" )
writeOutput( q.columnArray().len() )

```

Result: 2
