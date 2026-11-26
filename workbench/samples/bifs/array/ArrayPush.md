### Push a value onto an array

This is the full function version of arrayPush to push a value onto the end of the array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWK7EoqLEyoDS4gwNBTDTFkjqKJQl5pSm2poYKWhylRdllqT6l5YUlJZoKCiFZGQWQxQqZCQWKygpqEF4Pql5YAMUNIEiSgqpOam5qXklxXpKCpoAJMUjkA%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
]
arrayPush( array=arr, value=42 )
writeOutput( "This array has " & arrayLen( arr ) & " elements." )

```

Result: This array has 4 elements.

### Member function version.

Using the member function.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWK7EHKBgYlGRXkFpcYaGgomRgiZXeVFmSap%2FaUlBaYmGglJIRmYxSEVipUJGYrGCkoKaAlCTGpCRmpOam5pXUqynpKAJAH%2FiGcA%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
]
al = arr.push( 42 )
writeOutput( "This array has " & al & " elements." )

```

Result: This array has 4 elements.

### Push an object onto an array.

This demonstrates pushing an object onto an array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDijObi5DTk4ozVgTCVSsrzlRDcaiDmdFSwUjAGMmqB4lyxXIk5QK2JRUV6BaXFGRoKQGUmRlyxCppc5UWZJan%2BpSUFpSUaCkohGZnFIGWJlQoZicUKSgpqCkCdakBGak5qbmpeSbGekoImAMExIKo%3D" target="_blank">Run Example</a>

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
]
al = arr.push( [
	42
] )
writeOutput( "This array has " & al & " elements." )

```

Result: This array has 4 elements.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhiuVyKc0t0FBwLCpKrAwoLc7QUMiDKNdRMFDQBEJ9fQX%2F0pKC0pJiBVOu3PyiVD%2BEcVycpkBTzIDYHIgtsJmGpENHwQRkIheKkQDYLiq5" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
Dump( ArrayPush( numbers, 0 ) ) // Outputs 5
moreNumbers = [
	5,
	6,
	7,
	8
]
Dump( ArrayPush( moreNumbers, 4 ) )
 // Outputs 5

```


