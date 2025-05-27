### Member Function

Use the member function to return the first item from an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNuGKteYKL8osSfUvLSkoLdFQKIap1kvLLCou0dBU0LTmAgBj2hK%2B" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4
];
WriteOutput( someArray.first() );

```

Result: 1

### Non-Member Function

Return the first item from an array

<a href="https://try.boxlang.io/?code=eJwrTs3MS0vNSXEsKkqsVLBViFbg4lTySi0qqlTSAbJccxIz81LBTO%2BixNzUIjDTPTW%2FKD1ViSvWmiu8KLMk1b%2B0pKC0REMhEWSIW2ZRMZBdjGKwpoKmNRcA%2Bq4hVg%3D%3D" target="_blank">Run Example</a>

```java
seinfeldArray = [ 
	"Jerry",
	"Elaine",
	"Kramer",
	"George"
];
WriteOutput( arrayFirst( seinfeldArray ) );

```

Result: "Jerry"

### Additional Examples


```java
aNames = array( "Marcus", "Sarah", "Josefine" );
dump( var=aNames, label="aNames - original array" );
dump( var=arrayFirst( aNames ), label="first element of array aNames" );

```


