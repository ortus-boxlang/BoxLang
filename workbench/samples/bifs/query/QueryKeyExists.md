### Check to see if column exists in Query

Uses the member function syntax

<a href="https://try.boxlang.io/?code=eJxdjj0LwjAYhOfkVxyZWgiCjoKbjtXFTRyCfdXQmmryxhjE%2F278WHS7O%2B45zlEKmOESyeclpQrKtpot96R00Y7pQF5fjd8djS%2FRBlLcpRClpjDFWL%2F0p1%2BsmlOijJb2ZDhg7ePJOCXFQ%2F9Qkz%2BqMQ7J9F3A4NAMwxuRW9Qyecu0inyOXMGVr6OO8uJmA4dy9btQo34Ci8w54g%3D%3D" target="_blank">Run Example</a>

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
writeOutput( news.keyExists( "title" ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKyaXFJZ4uOiDKLzE3VUlBk6u8KLMk1aU0t0ADotA7tdK1IrO4pFhDIRdihI6CUmI6SC2xqpGMV9AEALANLoQ%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "custID,custName" )
writeDump( queryKeyExists( myQuery, "age" ) )
writeDump( queryKeyExists( myQuery, "custName" ) )

```

### Column does not exist

```java
writeOutput( queryKeyExists( queryNew( "id,name" ), "age" ) )

```

Result: false
