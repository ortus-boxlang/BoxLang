### Calculates the Median value

Uses the arrayMedian function to calculate the Median value

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYa5Ya65coGAiSNI3NSUzMU9DoRiuWtOaq7wosyTVv7SkoLREQyEXJAIARXgWXQ%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	2
]
m = arrayMedian( someArray )
writeOutput( m )

```

Result: 2

### Median of an even number of values

With an even number of elements the median is the average of the two middle values.

```java
writeOutput( arrayMedian( [ 1, 2, 3, 4 ] ) )
```

Result: 2.5

### Using the member function

The array is not modified and does not need to be sorted.

```java
numbers = [ 10, 2, 5 ]
writeOutput( numbers.median() )
```

Result: 5

### Additional Examples


```java
aNames = array( 10412, 42, 33, 2, 999, 12769, 888 )
dump( arrayMedian( aNames ) )
// member function
dump( aNames.median() )

```


