### Member Function

Use the member function to return the first item from an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNuGK5QovyixJ9S8tKSgt0VAohinWS8ssKi7R0FTQBAAv5RI%2B" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4
]
WriteOutput( someArray.first() )

```

Result: 1

### Non-Member Function

Return the first item from an array

<a href="https://try.boxlang.io/?code=eJwrTs3MS0vNSXEsKkqsVLBViFbg4lTySi0qqlTSAbJccxIz81LBTO%2BixNzUIjDTPTW%2FKD1ViSuWK7wosyTVv7SkoLREQyERZIZbZlExkF2MYq6mgiYAnEog1g%3D%3D" target="_blank">Run Example</a>

```java
seinfeldArray = [ 
	"Jerry",
	"Elaine",
	"Kramer",
	"George"
]
WriteOutput( arrayFirst( seinfeldArray ) )

```

Result: Jerry

### Default value for an empty array

The optional default value is returned when the array is empty.

```java
empty = []
writeOutput( empty.first( "none" ) )
```

Result: none

### Empty array without a default

An exception is thrown when the array is empty and no default value is given.

```java
try {
	arrayFirst( [] )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Cannot return first element of array; array is empty

### Additional Examples


```java
aNames = [ "Marcus", "Sarah", "Josefine" ]
dump( var=aNames, label="aNames - original array" )
dump( var=arrayFirst( aNames ), label="first element of array aNames" )

```


