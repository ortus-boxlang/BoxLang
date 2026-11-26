### Remove the last value from an array

This is the full function version of arrayPop to remove the last value of an array.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYhMjrliuAqBgYlFRYmVAfoEGhGULJBU0ucqLMktS%2FUtLCkpLNBQKFDQBF8ESPQ%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	42
]
p = arrayPop( array=arr )
writeOutput( p )

```

Result: 42

### Member function version.

Using the member function.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYhMjrliuAqBgYlGRXkF%2BgYYmV3lRZkmqf2lJQWmJhkKBgiYASqQN0A%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	42
]
p = arr.pop()
writeOutput( p )

```

Result: 42

### Default value for an empty array

The default value is returned when the array is empty.

```java
empty = []
writeOutput( empty.pop( "nothing left" ) )
```

Result: nothing left

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhiuVKKc0t0FBwLCpKrAzIB7LyoKo1gVBfX8G%2FtKSgtKRYwQSqECGNJBmtYKijYKSjYKwQy5WbX5Tqh7CRi9MUaJEZEJsDsQUWC5E06CiYoNtrAVWPbKwmF6rlpjoKZjoK5gqxAK00QaY%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
dump( ArrayPop( numbers ) ) // Outputs 4
dump( numbers ) // Outputs [ 1, 2, 3 ]
moreNumbers = [
	5,
	6,
	7,
	8
]
dump( ArrayPop( moreNumbers, 4 ) ) // Outputs 8
dump( moreNumbers )
 // Outputs [ 5, 6, 7 ]

```


