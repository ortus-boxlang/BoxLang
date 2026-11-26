### Reverse an Array

Creates a new array with reversed positions

<a href="https://try.boxlang.io/?code=eJzLrXQsKkqsVLBViFbg4jTU4eI0AmJjrliuXIhMUGpZalFxagpQRSISX0MBKq%2BgyVVelFmS6l9aUlBaoqHgFezvF5xalJmYk1mFUAU3RVNBEwAJ9CV4" target="_blank">Run Example</a>

```java
myArray = [ 
	1,
	2,
	3
]
myArrayReversed = arrayReverse( myArray )
writeOutput( JSONSerialize( myArrayReversed ) )

```

Result: [3,2,1]

### Reverse an Array via Member Function



<a href="https://try.boxlang.io/?code=eJzLrXQsKkqsVLBViFbg4jTU4eI0AmJjrliu8qLMklT%2F0pKC0hINBa9gf7%2Fg1KLMxJzMqlQNhVyILr2i1LLUouJUDU0FIAQAMjIWlg%3D%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	1,
	2,
	3
]
writeOutput( JSONSerialize( myArray.reverse() ) )

```

Result: [3,2,1]

### The original array is not modified

```java
original = [ 1, 2, 3 ]
reversed = original.reverse()
writeOutput( original.toList() & " | " & reversed.toList() )
```

Result: 1,2,3 | 3,2,1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNjjsOwjAQROvsKaZMJEOUT0vHCWijFAZvwBJeI8dG4vYYMIhiil29%2BUhyRw4rdphAVaeo6rOGrJFm0iHox4HvmeAaUtiG9snd%2Fm60LaZO9WpQ44wN4oXhgz1b0Ve8M2BXiI9w3tjFsqHscPyyY0lyitYLhU%2BPyVtK8ra86m%2FjD2meo105%2Fg%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
arrayReverse( numbers )
Dump( numbers ) // [1,2,3,4] - the original array is not modified
// member function
reversed = numbers.reverse()
Dump( reversed )

```
