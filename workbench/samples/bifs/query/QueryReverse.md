### Reverses a queries results



<a href="https://try.boxlang.io/?code=eJyF0D0LwkAMBuC59yvCTS0cih%2BTqIMIgoMOuonDtX2xRbxqvFOK%2BN%2B9Wh2qglsSnjeQZOACZxrRyYHLBa4hyTxVC32AVL40FjuwumhOMs1%2BtCER3EQQeCVpQB1V1U%2FuOzlhl4Am2hiwFMFdNXC3ideFKWllNe%2B%2Fae9jbxHHJU1Z7%2FFt%2B007hzY0Y5SVFFuKxJVzi6WzR2f9eesM9bWDYczUHsu3mLrDMaSs%2FsivFOMCPiP9H2%2B9aBhR9ACKqWLx" target="_blank">Run Example</a>

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
] )
writeOutput( "The query:<br />" )
writeDump( heroes )
writeOutput( "The reversed query:<br />" )
writeDump( heroes.reverse() )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx9TkEKwjAQPCevWPbUQkEpnooKBcWTFVFP4iHWxfaQUjdJi4h%2FN1UrCOJph5nZmbFk7NoRX2ECl%2B5m1AaAldIEEagzYQTYKM4LxZ6onCYuc0%2FeQIosXc4hgb0UAjfOlBh1aMcGpTh4nC56OR52UjzyvLxDKFsuLa2crZ31bduCXuXJ%2BMgwmGLvmDldB2A%2FG389MjXEhk7%2FEp7S2%2FiVB%2BEDNURNQA%3D%3D" target="_blank">Run Example</a>

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
writeOutput( "The query:<br />" )
writeDump( testQuery )
writeOutput( "The reversed query:<br />" )
writeDump( queryreverse( testQuery ) )

```

### Reverse the rows

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ] ] )
writeOutput( arrayToList( queryColumnData( queryReverse( q ), "id" ) ) )

```

Result: 3,2,1

### The original query is not modified

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ], [ 3 ] ] )
reversed = q.reverse()
writeOutput( arrayToList( q.columnData( "id" ) ) )

```

Result: 1,2,3
