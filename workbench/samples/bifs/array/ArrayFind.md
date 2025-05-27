### Find an "Apple" in an array of fruit

Returns the index of the element "Apple" in the array

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSCwqSqx0y8xL8ct3TixO1VCIVuDiVMovSsxLT1XSATILMvNSEwsKciA8CIsrVkdByRHMVNBU0LTmAgCKPhkK" target="_blank">Run Example</a>

```java
writeOutput( arrayFindNoCase( [ 
	"orange",
	"pineapple",
	"apple"
], "Apple" ) );

```

Result: 3

### arrayFind is not Case Sensitive

Not case sensitive so "Apple" will be found in the array, returns 1. Use arrayFind for case sensitive matching.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSCwqSqx0y8xL8ct3TixO1VCIVuDiVMovSsxLT1XSATILMvNSEwsKciA8CIsrVkdByRHMVNBU0LTmAgCKPhkK" target="_blank">Run Example</a>

```java
writeOutput( arrayFindNoCase( [ 
	"orange",
	"pineapple",
	"apple"
], "Apple" ) );

```

Result: 1

### Member Functions: Find an "Apple" in an array of fruit

Calls the findNoCase member function of the array object. Requires CF11+ or Lucee4.5+

<a href="https://try.boxlang.io/?code=eJxLKyrNLFGwVYhW4OJUyi9KzEtPVdIBMgsy81ITCwpyIDwIiyvWmqu8KLMk1b%2B0pKC0REMhDaRZLy0zL8Uv3zmxOFVDQckRrFJBU0HTmgsAGFQcAw%3D%3D" target="_blank">Run Example</a>

```java
fruit = [ 
	"orange",
	"pineapple",
	"apple"
];
writeOutput( fruit.findNoCase( "Apple" ) );

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLLCoKSyzKTEzKSVWwVYhW4OJUCk8sSS1S0gGygrMrQbQRiO2YWaTEFWvNFV6UWZLqUppboKHgWFSUWOmWmZfil%2B%2BcWJyqoZCIMExHwVRBU0HTWkFfX8G%2FtKSgtKRYwYB4zUqJQOswDDAh3gAjsGYuZN3GXAAzo0hq" target="_blank">Run Example</a>

```java
arrVariable = [ 
	"Water",
	"Sky",
	2,
	"Air"
];
WriteDump( ArrayFindNoCase( arrVariable, 5 ) ); // Outputs 0
WriteDump( ArrayFindNoCase( arrVariable, "air" ) ); // Outputs 4
WriteDump( ArrayFindNoCase( arrVariable, 2 ) );
 // Outputs 3

```


