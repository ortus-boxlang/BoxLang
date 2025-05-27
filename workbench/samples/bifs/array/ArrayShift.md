### Example with simple values

Take an array of numbers and shift the first one off.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuu1BygaGJRUWJlcEZmWokGiK2gac1VXpRZkupfWlJQChQDKgIKAQAmphH6" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
el = arrayShift( arr );
writeOutput( el );

```

Result: 1

### Using a member function

This is the same example as above, but using a member function on the array instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuu1BygaGJRkV5xRmZaiYamNVd5UWZJqn9pSUFpiYYCUBooBAC5dg%2Fp" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
el = arr.shift();
writeOutput( el );

```

Result: 1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmciwqSqwMzshMK9FQyIMq1bTmcinNLUAWUNDXV%2FAvLSkoLSlWMOTKzS9K9UOYy8VpCjTODIjNgdgC3Vhk1QSMNuXKQzZWKT8vVQlopFJJeT6EzihKhYik5ZcWKZHgAS5ka4DGcgEAWb5Q%2BA%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
ArrayShift( numbers );
Dump( numbers ); // Outputs 1
moreNumbers = [
	5,
	6,
	7,
	8
];
ArrayShift( moreNumbers );
Dump( numbers ); // Outputs 5
numbers = [
	"one",
	"two",
	"three",
	"four"
];
ArrayShift( numbers );
Dump( numbers );
 // Outputs one

```


