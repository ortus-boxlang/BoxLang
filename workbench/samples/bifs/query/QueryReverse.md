### Reverses a queries results



<a href="https://try.boxlang.io/?code=eJyN0DsLwjAQB%2FC5%2BRRHpgpB8TH5GqQgONRBN3FI9Y8VMdVrUinidzdVEaoObvf43cFdCs6Q04jODlzGuIQk91sV6yOk8qGx2IFVoXmTavalFYngKoLAK0l9aqsqfnCfyQm7DWiijQFLEdxUDXfqeJmZkhZW8%2BGbdj%2F2ZklSUsT6gG%2Fbq9sZtKEpo6ykWFNjIC68t5g7e3LW37dM8Ty3P0yYWmP5JpE7nkJKnz%2F5OccowDm2fyxovmzYqNp3vihkIg%3D%3D" target="_blank">Run Example</a>

```java
heroes = queryNew( "id,Name", "integer,varchar", [ 
	{
		"id" : 1,
		"Name" : "Bruce Banner"
	},
	{
		"id" : 2,
		"Name" : "Tony Stark"
	},
	{
		"id" : 3,
		"Name" : "Bobby Drake"
	},
	{
		"id" : 4,
		"Name" : "Jean Grey"
	}
] );
writeOutput( "The query:<br />" );
writeDump( heroes );
writeOutput( "The reversed query:<br />" );
writeDump( heroes.reverse() );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrSS0uCSxNLapUsFUoBNF%2BqeUaCkp5ibmpCjoKiempSjoKSmWJRckZiUVAgbzS3NSizGSgYLUCF6efo6%2BrgpVCNBcnp1JwaXGmkg6IFVpUrMTFGQtkO7rDpI0MQFJGJkBxrloFTWuu8qLMklT%2F0pKC0hKgdSEZqRDbrWySihT07ZTgSlxKcws0FErgrsSqtSi1LLWoODUFrxlgOahKFBNB6gDfPE5x" target="_blank">Run Example</a>

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
} );
writeOutput( "The query:<br />" );
writeDump( testQuery );
writeOutput( "The reversed query:<br />" );
writeDump( queryreverse( testQuery ) );

```


