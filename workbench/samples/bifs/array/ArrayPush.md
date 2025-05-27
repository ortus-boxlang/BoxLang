### Push a value onto an array

This is the full function version of arrayPush to push a value onto the end of the array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuuxKKixMqA0uIMDQUw0xZI6iiUJeaUptqaGCloWnOVF2WWpPqXlhSUlmgoKIVkZBZDVCpkJBYrKCmoQXg%2BqXlgExQ0gSJKCqk5qbmpeSXFekogIwCmqCRL" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
arrayPush( array=arr, value=42 );
writeOutput( "This array has " & arrayLen( arr ) & " elements." );

```

Result: This array has 4 elements.

### Member function version.

Using the member function. This version also works in ACF2018.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWGuuxBygaGJRkV5BaXGGhoKJkYKmNVd5UWZJqn9pSUFpiYaCUkhGZjFISWKlQkZisYKSgpoCUJcakJGak5qbmldSrKcE0gUA358aew%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
];
al = arr.push( 42 );
writeOutput( "This array has " & al & " elements." );

```

Result: This array has 4 elements.

### Push an object onto an array.

This demonstrates pushing an object onto an array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDijObi5DTk4ozVgTCVSsrzlRDcaiDmdFSwUjAGMmqB4lyx1lyJOUC9iUVFegWlxRkaCkB1JkZcsQqa1lzlRZklqf6lJQWlJRoKSiEZmcUgdYmVChmJxQpKCmoKQK1qQEZqTmpual5JsZ4SSBcAPPchZQ%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	[
		1
	],
	[
		"two"
	],
	[
		{
			A : 3
		}
	]
];
al = arr.push( [
	42
] );
writeOutput( "This array has " & al & " elements." );

```

Result: This array has 4 elements.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmcinNLdBQcCwqSqwMKC3O0FDIg6jXUTBQ0FTQtFbQ11fwLy0pKC0pVjDlys0vSvVDGMjFaQo0xwyIzYHYAqt5SFp0FEzAZnKhGgoAoF8rrw%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
Dump( ArrayPush( numbers, 0 ) ); // Outputs 5
moreNumbers = [
	5,
	6,
	7,
	8
];
Dump( ArrayPush( moreNumbers, 4 ) );
 // Outputs 5

```


