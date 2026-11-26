### Show the last element of an array

Uses the arrayLast function to retrieve the last element of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVErLLCouUdIBsopTk%2FPzUsDMkozMohQlrliunMTiEv%2B8VKDiRJAmHyBXQ6EYboYmV3lRZkmqf2lJQSlQAqZaEwBSHiCw" target="_blank">Run Example</a>

```java
someArray = [ 
	"first",
	"second",
	"third"
]
lastOne = arrayLast( someArray )
writeOutput( lastOne )

```

Result: third

### Using the member function

```java
names = [ "Marcus", "Sarah", "Josefine" ]
writeOutput( names.last() )
```

Result: Josefine

### Empty array

An exception is thrown when the array is empty.

```java
try {
	arrayLast( [] )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Cannot return last element of array; array is empty

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLS8xNLVawVYhW4OJU8k0sSi4tVtIBMoMTixIzwCyv%2FOLUtMy8VCWuWK6U0twCDYU8kB69nMTiEg1NBU0uBX19hfzSkoLSkmIFdZhqdQBxnxpj" target="_blank">Run Example</a>

```java
names = [ 
	"Marcus",
	"Sarah",
	"Josefine"
]
dump( names.last() )
 // outputs 'Josefine'

```



```java
names = [ "Marcus", "Sarah", "Josefine" ]
dump( arrayLast( names ) )
 // outputs 'Josefine'

```


