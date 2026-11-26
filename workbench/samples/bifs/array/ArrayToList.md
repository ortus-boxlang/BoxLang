### Retrieve an array as a list

Uses the arrayToList function with a pipe delimiter to retrieve an array as a list

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNuGK5SoGyvtkFpcApRNBykLyQTwNhWKYPh0FpRolBU2u8qLMklT%2F0pKCUqgsWJcmABv6HPk%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4
]
someList = arrayToList( someArray, "|" )
writeOutput( someList )

```

Result: 1|2|3|4

### Retrieve an array as a list using the Array member function

 Uses the Array member function to retrieve an array as a list

<a href="https://try.boxlang.io/?code=eJwrTs3MS0vNSXEsKkqsVLBViFbg4lTySi0qqlTSAbJccxIz81LBTO%2BixNzUIjDTPTW%2FKD1ViSuWqxiq3SezuASouxjZNL2SfJCwhiZXeVFmSap%2FaUlBaYmGAooWTQD6kimP" target="_blank">Run Example</a>

```java
seinfeldArray = [ 
	"Jerry",
	"Elaine",
	"Kramer",
	"George"
]
seinfeldList = seinfeldArray.toList()
writeOutput( seinfeldList )

```

Result: Jerry,Elaine,Kramer,George

### Using the join member function

`join` is an alias of `toList`.

```java
writeOutput( [ "a", "b", "c" ].join( "-" ) )
```

Result: a-b-c

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNzDEOgzAQRNE6PsVoK5AgnICCPmW6KMVijOLCNlqvhXL7BAQSzdc086JbBxH%2BoscL5kZMzb%2FjXnvZ02W7vTOZt5lKWCrwJjzTw2etEE%2BxRm26DsGF0QnmEq36FBusXj%2BwJWsKyG5hYU1ySOf5rodGbUub9AMMKS8H" target="_blank">Run Example</a>

```java
newArray = [ 
	"a",
	"b",
	"c",
	"b",
	"d",
	"b",
	"e",
	"f"
]
dump( arrayToList( newArray ) )
// member function, with custom separator
dump( newArray.toList( "--" ) )

```


