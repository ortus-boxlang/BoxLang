### Simple example for arrayDeleteAt function

Uses the arrayDeleteAt function to delete the value in specific position

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVApKTVHSAdLhGZklqWCWe1Fqah6Y5ZRTChEKyMzLVuKKteZKBGl1Sc1JLUl1LNFQKIaZpqNgrKBpzVVeBDTEv7SkoBQo6RXs7xecWpSZmJNZlYqkVkETpBQAhJIoyQ%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
];
arrayDeleteAt( someArray, 3 );
writeOutput( JSONSerialize( someArray ) );

```

Result: ["Red", "White", "Blue", "Pink"]

### Simple example with member function

Uses the member function is the same as running arrayDeleteAt.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVApKTVHSAdLhGZklqWCWe1Fqah6Y5ZRTChEKyMzLVuKKteYqhmnXc0nNSS1JdSzRUDBS0LTmKi8CavcvLSkoBYp4Bfv7BacWZSbmZFalaijANSlogpQCAIUrJos%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
];
someArray.DeleteAt( 2 );
writeOutput( JSONSerialize( someArray ) );

```

Result: ["Red", "Green", "Blue", "Pink"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNDTS4eI0AGJDIAaxjYHYBIhNgdiMK9aaK6U0t0BDIRGoQ9OaC0glVrqk5qSWpDqWgEV1FExAEiiqAJteFeI%3D" target="_blank">Run Example</a>

```java
arr = [ 
	12,
	0,
	1,
	2,
	3,
	4,
	5,
	6
];
dump( arr );
arrayDeleteAt( arr, 4 );
dump( arr );

```


<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNDTS4eI0AGJDIAaxjYHYBIhNgdiMK9aaK6U0t0BDIRGoQ9OaC0jppaTmpJakOpZoKJiAhFDkAQ8KE8U%3D" target="_blank">Run Example</a>

```java
arr = [ 
	12,
	0,
	1,
	2,
	3,
	4,
	5,
	6
];
dump( arr );
arr.deleteAt( 4 );
dump( arr );

```


