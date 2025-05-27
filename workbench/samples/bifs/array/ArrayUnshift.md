### Example with simple values

Add a new element to an array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuuvNRyx6Iin9Q8oGRiUVFiZWhecUZmWokGiKejYKCgac1VXpRZkupfWlJQChRGaADKAADbtBjT" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
newArrLen = arrayUnshift( arr, 0 );
writeOutput( newArrLen );

```

Result: 4

### Using a member function

This is the same example as above, but using a member function on the array instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuuvNRyx6Iin9Q8oGRiUZFeaV5xRmZaiYaCgYKmNVd5UWZJqn9pSUEpUAShFCgDAEJoFrY%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
newArrLen = arr.unshift( 0 );
writeOutput( newArrLen );

```

Result: 4

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmcinNLdBQcCwqSqwMzQvOyEwr0VDIg2jRUTBQ0FTQtFbQ11fwLy0pKC0pVjDlys0vSvVDmMnFaQo0ygyIzYHYApeRSLp0FEzAxnKhmgsA2rkt8Q%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
Dump( ArrayUnShift( numbers, 0 ) ); // Outputs 5
moreNumbers = [
	5,
	6,
	7,
	8
];
Dump( ArrayUnShift( moreNumbers, 4 ) );
 // Outputs 5

```


