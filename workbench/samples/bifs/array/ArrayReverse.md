### Reverse an Array

Creates a new array with reversed positions

<a href="https://try.boxlang.io/?code=eJzLrXQsKkqsVLBViFbg4jTU4eI0AmJjrlhrrlyIVFBqWWpRcWoKUEkiEl9DASqvoGnNVV6UWZLqX1pSUFqioeAV7O8XnFqUmZiTWYVQBjdGE6QBAJDEJjM%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	1,
	2,
	3
];
myArrayReversed = arrayReverse( myArray );
writeOutput( JSONSerialize( myArrayReversed ) );

```

Result: [3,2,1]

### Reverse an Array via Member Function



<a href="https://try.boxlang.io/?code=eJzLrXQsKkqsVLBViFbg4jTU4eI0AmJjrlhrrvKizJJU%2F9KSgtISDQWvYH%2B%2F4NSizMSczKpUDYVciDa9otSy1KLiVA1NBSC05gIAcYEXFg%3D%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	1,
	2,
	3
];
writeOutput( JSONSerialize( myArray.reverse() ) );

```

Result: [3,2,1]

### Reverse an Array using array slice syntax

Reverse an Array using array slice syntax adding in Boxlang 2018


```java
myArray = [1,2,3]; 
writeOutput( serializeJSON( myArray[::-1] ) );
```

Result: [3,2,1]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmSiwqSqwMSi0DKknVUMiDKta05nIpzS1AFlDQ11eINtEx1jHSMYzlAnJyU0FSCmmlecklmfl5XEUQQ1KANkF16UGFNODGwdUARQAU9S3%2F" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
arrayReverse( numbers );
Dump( numbers ); // [4,3,2,1]
// member function
reversed = numbers.reverse();
Dump( reversed );

```


