### Check to see if column exists in Query

Uses the member function syntax

<a href="https://try.boxlang.io/?code=eJxdjrEKwjAURefkKy6ZWgiCjoqbjtXFTRyCfWpoTTV5MRbx303VRbd7H%2Bc8rqMUMMc1ku9XlAooW2u23JLSOTumI3l9M35%2FMj6ftpDiIYXImMIUYz3kD5%2BrWlCiHjUdyHDAxsezcUqKp%2F6xJn9WZRySaZuAzqHqurcidyhnMnnLtI58iVzA5bGjhvrl3QYOeev3RTmQLzYjOmI%3D" target="_blank">Run Example</a>

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
] );
writeOutput( news.keyExists( "title" ) );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKyaXFJZ4uOiDKLzE3VUlB05qrvCizJNWlNLdAA6LSO7XStSKzuKRYQyEXYoaOglJiOkgx0cqRLADpAQBbsC8%2F" target="_blank">Run Example</a>

```java
myQuery = queryNew( "custID,custName" );
writeDump( queryKeyExists( myQuery, "age" ) );
writeDump( queryKeyExists( myQuery, "custName" ) );

```


