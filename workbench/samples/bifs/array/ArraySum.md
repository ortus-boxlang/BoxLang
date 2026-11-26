### Sum of values in an array

Uses the arraySum function to get sum of values in an array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3Igrlqu8KLMk1b%2B0pKC0REMhEaQ4uDRXQyEPSbOmgiYAKx8WIQ%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
]
writeOutput( arraySum( numberArray ) )

```

Result: 208

### Sum of values in an array

To get sum of values in an empty array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYiO5SovyixJ9S8tKSgt0VBIBIkHl%2BZqKOQhqdNU0AQANQ4Tgg%3D%3D" target="_blank">Run Example</a>

```java
numberArray = []
writeOutput( arraySum( numberArray ) )

```

Result: 0

### Sum of values in an array

 Uses the sum member function is the same as running arraySum.

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3Igrlqu8KLMk1b%2B0pKC0REMhD6FFr7g0V0NTQRMAqhsUEA%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
]
writeOutput( numberArray.sum() )

```

Result: 208

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3IgrliulNLdAQyERpCq4NFdDIQ9Jl6aCJpe%2BvkJuKkhIIa00L7kkMz8PqgVJoV4xUCdQMQA9%2FCJO" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
]
dump( arraySum( numberArray ) )
// member function
dump( numberArray.sum() )

```


