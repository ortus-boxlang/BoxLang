### Find an "Apple" in an array of fruit

Returns the index of the element "Apple" in the array

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSCwqSqx0y8xL8ct3TixO1VCIVuDiVMovSsxLT1XSATILMvNSEwsKciA8CIsrVkdByRHMVNBU0AQAWDQYxQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( arrayFindNoCase( [ 
	"orange",
	"pineapple",
	"apple"
], "Apple" ) )

```

Result: 3

### arrayFind is Case Sensitive

arrayFind compares case sensitively, so "Apple" is not found in the array and 0 is returned. Use arrayFindNoCase for case insensitive matching.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSCwqSqx0y8xL0VCIVuDiVMovSsxLT1XSATILMvNSEwsKciA8CIsrVkdByRHMVNBU0AQApwEWjA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( arrayFind( [ 
	"orange",
	"pineapple",
	"apple"
], "Apple" ) )

```

Result: 0

### Member Functions: Find an "Apple" in an array of fruit

Calls the findNoCase member function of the array object.

<a href="https://try.boxlang.io/?code=eJxLKyrNLFGwVYhW4OJUyi9KzEtPVdIBMgsy81ITCwpyIDwIiyuWq7wosyTVv7SkoLREQyENpFcvLTMvxS%2FfObE4VUNByRGsUEFTQRMAyPIbgw%3D%3D" target="_blank">Run Example</a>

```java
fruit = [ 
	"orange",
	"pineapple",
	"apple"
]
writeOutput( fruit.findNoCase( "Apple" ) )

```

Result: 3

### Find with a function

When the value is a function it is used as the search predicate and the index of the first element it returns true for is returned.

```java
numbers = [ 5, 8, 12, 3 ]
writeOutput( numbers.find( ( value ) -> value > 10 ) )
```

Result: 3

### Contains returns a boolean

The `contains` variants return true or false instead of an index. `contains` is case sensitive and `containsNoCase` is not.

```java
letters = [ "a", "b" ]
writeOutput( letters.contains( "B" ) & "," & letters.containsNoCase( "B" ) )
```

Result: false,true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLLCoKSyzKTEzKSVWwVYhW4OJUCk8sSS1S0gGygrMrQbQRiO2YWaTEFcsVXpRZkupSmlugoeBYVJRY6ZaZl%2BKX75xYnKqhkIgwS0fBVEETCPX1FfxLSwpKS4oVDIjXq5QItAxdvwnx%2Bo1AermQNRsDAO8%2FR3Q%3D" target="_blank">Run Example</a>

```java
arrVariable = [ 
	"Water",
	"Sky",
	2,
	"Air"
]
WriteDump( ArrayFindNoCase( arrVariable, 5 ) ) // Outputs 0
WriteDump( ArrayFindNoCase( arrVariable, "air" ) ) // Outputs 4
WriteDump( ArrayFindNoCase( arrVariable, 2 ) )
 // Outputs 3

```


