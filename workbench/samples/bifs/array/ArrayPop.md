### Remove the last value from an array

This is the full function version of arrayPop to remove the last value of an array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYhMjrlhrrgKgaGJRUWJlQH6BBoRlCyQVNK25yosyS1L9S0sKSks0FApAIgBcoRL4" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	42
];
p = arrayPop( array=arr );
writeOutput( p );

```

Result: 42

### Member function version.

Using the member function. This version also works in ACF2018.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYhMjrlhrrgKgaGJRkV5BfoGGpjVXeVFmSap%2FaUlBaYmGQoECUAQAf3kOiw%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	42
];
p = arr.pop();
writeOutput( p );

```

Result: 42

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjb0Kg0AQhOvbp5gyBwuinj9wWOQFEnuxSIjlqZxe4du7xZEopvhgYWb2G4N7D35Bgw6kUiaVCblgqLf0CW6%2B4e79a2snucZY19AWSYJnWOewLjCx%2Bc1PaYeUkTFy9OQmPzx%2BUlKFuEqhEup%2FzsOCYS7qOg6Oj7Wls79glIxK%2FDtWokMS" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
dump( ArrayPop( numbers ) ); // Outputs 4
dump( numbers ); // Outputs [ 1, 2, 3 ]
moreNumbers = [
	5,
	6,
	7,
	8
];
dump( ArrayPop( moreNumbers, 4 ) ); // Outputs 8
dump( moreNumbers );
 // Outputs [ 5, 6, 7 ]

```


