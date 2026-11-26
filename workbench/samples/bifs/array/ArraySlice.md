### Simple arraySlice Example



<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA2AWJTIDYDYnMgtuCK5cpLLXeEKk8E0cE5mcmpGhC2joKRjoKxgiZXeVFmSap%2FaUlBaYmGglewv19walFmYk5mFVAl3ABNBU0Aztsgjg%3D%3D" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4,
	5,
	6,
	7,
	8
]
newArray = arraySlice( array, 2, 3 )
writeOutput( JSONSerialize( newArray ) )

```

Result: [2,3,4]

### arraySlice with no length specified



<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA2AWJTIDYDYnMgtuCK5cpLLXeEKk8E0cE5mcmpGhC2joKJgiZXeVFmSap%2FaUlBaYmGglewv19walFmYk5mFVAZXLemgiYAg84gEQ%3D%3D" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4,
	5,
	6,
	7,
	8
]
newArray = arraySlice( array, 4 )
writeOutput( JSONSerialize( newArray ) )

```

Result: [4,5,6,7,8]

### arraySlice using a negative offset



<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA2AWJTIDYDYnMgtuCK5cpLLXeEKk8E0cE5mcmpGhC2joKuqY6CsYImV3lRZkmqf2lJQWmJhoJXsL9fcGpRZmJOZhVQKdwETQVNAOlZIL4%3D" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4,
	5,
	6,
	7,
	8
]
newArray = arraySlice( array, -5, 3 )
writeOutput( JSONSerialize( newArray ) )

```

Result: [4,5,6]

### Slice an array using member function

Calling the slice member function on an array.

<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA2AWJTIDYDYnMgtuCK5cpLLXeEKk8E0XrFOZnJqRoKRjoKxgqaXOVFmSWp%2FqUlBaUlGgpewf5%2BwalFmYk5mVVAJXCtmgqaAAYoHnE%3D" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4,
	5,
	6,
	7,
	8
]
newArray = array.slice( 2, 3 )
writeOutput( JSONSerialize( newArray ) )

```

Result: [2,3,4]

### Negative length

A negative length leaves out that many elements from the end of the array.

```java
numbers = [ 1, 2, 3, 4, 5, 6, 7, 8 ]
writeOutput( numbers.slice( 2, -2 ).toList() )
```

Result: 2,3,4,5,6

### Using the arrayMid alias

```java
writeOutput( arrayMid( [ 1, 2, 3, 4 ], 2, 2 ).toList() )
```

Result: 2,3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVYhW4OJUSlTSAZJJYDIZiZ2CxE4Fk2lKXLFcKaW5BRoKeTBDNLkyEouD83NTDYHmJYKEgnMyk1MRKnQUDHUUTIDqIBrhqjW59PUVclNzk1KLFNJK85JLMvPzYGYZAc2C6dcrhphnrKNghm6KkYImAEdhOTc%3D" target="_blank">Run Example</a>

```java
newArray = [ 
	"a",
	"b",
	"c",
	"b",
	"d",
	"b",
	"e",
	"f"
]
dump( newArray )
hasSome1 = arraySlice( newArray, 1, 4 )
dump( hasSome1 )
// member function
hasSome2 = newArray.slice( 3, 6 )
dump( hasSome2 )

```


