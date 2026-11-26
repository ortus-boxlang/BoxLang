### Simple example for arrayDeleteAt function

Uses the arrayDeleteAt function to delete the value in specific position

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVApKTVHSAdLhGZklqWCWe1Fqah6Y5ZRTChEKyMzLVuKK5UoE6XRJzUktSXUs0VAohhmmo2CsoMlVXgQ0wr%2B0pKAUKOcV7O8XnFqUmZiTWZWKpFRBU0ETAO7WKA4%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
]
arrayDeleteAt( someArray, 3 )
writeOutput( JSONSerialize( someArray ) )

```

Result: ["Red","White","Blue","Pink"]

### Simple example with member function

Uses the member function is the same as running arrayDeleteAt.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVApKTVHSAdLhGZklqWCWe1Fqah6Y5ZRTChEKyMzLVuKK5SqG6dZzSc1JLUl1LNFQMFLQ5CovAmr2Ly0pKAUKeAX7%2BwWnFmUm5mRWpWoowPUoaCpoAgD3iyXQ" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
]
someArray.DeleteAt( 2 )
writeOutput( JSONSerialize( someArray ) )

```

Result: ["Red","Green","Blue","Pink"]

### Index out of bounds

An index lower than 1 or greater than the array size throws an exception.

```java
try {
	arrayDeleteAt( [ 1, 2 ], 5 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Index [5] out of bounds for list with 2 elements.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNDTS4eI0AGJDIAaxjYHYBIhNgdiMK5YrpTS3QEMhEahBkwtIJla6pOaklqQ6loAFdRRMgOJIagA2xBTs" target="_blank">Run Example</a>

```java
arr = [ 
	12,
	0,
	1,
	2,
	3,
	4,
	5,
	6
]
dump( arr )
arrayDeleteAt( arr, 4 )
dump( arr )

```


<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNDTS4eI0AGJDIAaxjYHYBIhNgdiMK5YrpTS3QEMhEahBkwtI6qWk5qSWpDqWaCiYAEWQZAGzfBLP" target="_blank">Run Example</a>

```java
arr = [ 
	12,
	0,
	1,
	2,
	3,
	4,
	5,
	6
]
dump( arr )
arr.deleteAt( 4 )
dump( arr )

```


