### Sum of values in an array

Uses the arraySum function to get sum of values in an array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3Igr1pqrvCizJNW%2FtKSgtERDIRGkOrg0V0MhD0m3poKmNRcAaewWoQ%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
];
writeOutput( arraySum( numberArray ) );

```

Result: 208

### Sum of values in an array

To get sum of values in an empty array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYiOteYqL8osSfUvLSkoLdFQSARJBJfmaijkISnUVNC05gIAa%2F4UAg%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [];
writeOutput( arraySum( numberArray ) );

```

Result: 0

### Sum of values in an array

 Uses the sum member function is the same as running arraySum.

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3Igr1pqrvCizJNW%2FtKSgtERDIQ%2BhR6%2B4NFdDU0HTmgsA42QUkA%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
];
writeOutput( numberArray.sum() );

```

Result: 208

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI0NNDh4rS0BBJG5kDC3Igr1porpTS3QEMhEaQsuDRXQyEPSZumgqY1l76%2BQm4qSEwhrTQvuSQzPw%2BqB0mlXjFQK1g1ALxLIwk%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	10,
	99,
	27,
	72
];
dump( arraySum( numberArray ) );
// member function
dump( numberArray.sum() );

```


