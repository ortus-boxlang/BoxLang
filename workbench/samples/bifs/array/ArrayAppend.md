### Append a value to an array

Uses the arrayAppend function to append a value to the end of the array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWK5EkLhjQUFqXoqGQjFMoY6CiYImV3lRZkmqf2lJQWmJhoJXsL9fcGpRZmJOZlUqklIFTQVNAHp3Hfw%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
]
arrayAppend( someArray, 4 )
writeOutput( JSONSerialize( someArray ) )

```

Result: [1,2,3,4]

### Append a value to an array using the Array member function

Invoking the append function on an array is the same as running arrayAppend.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWK5imJxeYkFBal6KhoKJgiZXeVFmSap%2FaUlBaYmGglewv19walFmYk5mVaqGAlyHgqaCJgDGVxvf" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
]
someArray.append( 4 )
writeOutput( JSONSerialize( someArray ) )

```

Result: [1,2,3,4]

### Append more than one item

You can merge two arrays when third parameter is set to true.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWC6wuGNBQWpeioZCMUyhjkI0F6cJUIkpEJtxxeoolBSVpipocpUXZZakupTmFmgoeAX7%2BwWnFmUm5mRWpSLpVdBU0AQABlwgqw%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
]
ArrayAppend( someArray, [
	4,
	5,
	6
], true )
writeDump( JSONSerialize( someArray ) )

```

Result: [1,2,3,4,5,6]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kD0LwjAQhufer3hHCwelH2m7dCg46w8oGRQzJoY0Gfz3nggaKAoODs9y997dw7lkzyasmLCAipqpaIRW6EjTHMLpNntv3GUH94wyFEraJ%2BtfJZSoKhxT9CmusqhmNIyW0T3Cmtz7yPaGvQZzyANKGr0wCONHiXzsB50FitEzBsYI%2FX83RgzJbA3py8cywTuF4m0b" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
ArrayAppend( numbers, 5 )
Dump( numbers ) // Outputs [ 1, 2, 3, 4, 5 ]
numbers = [
	1,
	2,
	3,
	4
]
moreNumbers = [
	5,
	6,
	7,
	8
]
ArrayAppend( numbers, moreNumbers )
Dump( numbers ) // Outputs [ 1, 2, 3, 4, [ 5, 6, 7, 8 ] ]
numbers = [
	1,
	2,
	3,
	4
]
moreNumbers = [
	5,
	6,
	7,
	8
]
ArrayAppend( numbers, moreNumbers, true )
Dump( numbers )
 // Outputs [ 1, 2, 3, 4, 5, 6, 7, 8 ]

```


