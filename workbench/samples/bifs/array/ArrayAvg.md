### Average value of an array

Uses the arrayAvg function to get average value of an array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65YrvKizJJU%2F9KSgtISDYVEkCLHsnQNhTwkTZoKmgC3HRS8" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
]
writeOutput( arrayAvg( numberArray ) )

```

Result: 2

### Average value of an array using the Array member function

Uses the avg member function is the same as running arrayAvg.

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65YrvKizJJU%2F9KSgtISDYU8hFK9xLJ0DU0FTQA%2FJxKr" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
]
writeOutput( numberArray.avg() )

```

Result: 2

### Average of an empty array

An empty array has no average, so dividing by zero throws an exception.

```java
try {
	arrayAvg( [] )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: You cannot divide by zero.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhiuVKLEsPS8wpTQVKOxYVJVY6lqVrKORBtWhyuSZn5GsowBVpcino6yvkl5YUlJYUKxjpmQIAlDoaOA%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
avgValue = ArrayAvg( numbers )
Echo( avgValue )
 // outputs 2.5

```


