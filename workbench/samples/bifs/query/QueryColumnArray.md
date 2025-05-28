### Dump array of query columns



<a href="https://try.boxlang.io/?code=eJxdjjELwjAQhefkVxyZWsiio%2BIgutbJTRwOe2qxveglMQTxv5vaSbf3Ht8Hjyl5WMEjkuQdpQpM19rQhZ6MLZkDXUjsE%2BV0RSnTAbR6aaUKZmABMzvmiS%2FVbClRhpbOhMHDXuKAbLR62x9r%2Fmc1yJCwv3lwDI1zX0UfoV7qNg73arq3cX0ceC2CuQIef9cj8QFbuDei" target="_blank">Run Example</a>

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
dump( queryColumnArray( news ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUILE0tqvRLLddQUEpMTFRS0LTmSiwqMgTKRMdac4FlHVNSnPNzSnPzNBQKiyp1FJSSkpR0FMCqgKrLizJLUl1KcwuAsiDVEKWORUWJlWD1CpogVQBQVSJD" target="_blank">Run Example</a>

```java
qry = QueryNew( "aaa" );
arr1 = [];
QueryAddColumn( qry, "bb", arr1 );
writeDump( queryColumnArray( qry ) );

```


