### Simple example

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNgFiU65Ya67yosySVP%2FSkoLSEg2FRJBqz2KX1LTMvNQUDYVimAk6CsYKmgqa1lwAtkgXRQ%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4,
	5
]
writeOutput( arrayIsDefined( someArray, 3 ) )

```

Result: true

### Simple example

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNgFiU65Ya67yosySVP%2FSkoLSEg2FRJBqz2KX1LTMvNQUDYVimAk6CmYKmgqa1lwAtl0XSA%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4,
	5
]
writeOutput( arrayIsDefined( someArray, 6 ) )

```

Result: false

### Simple example with two dimensional array

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJxzyywqLnEsKkqsVLBViFbg4jTU4eI0AmJjrlhrruLU5Py8FLg0UBYkbQiSNwQrSM7PTcrMS4WpSATRfqnlGgpGCprWXGCuY0FBal6KhgKyUh2FNIS9%2BBUiOwGosrwosyTVv7SkoLREA2KdZ7FLahpQPYZOYwVNkA4AntpGyg%3D%3D" target="_blank">Run Example</a>

```java
FirstArray = [ 
	1,
	2,
	3
]
secondArray = [
	11,
	12,
	13
]
combineArray = arrayNew( 2 )
arrayAppend( combineArray, firstArray )
arrayAppend( combineArray, secondArray )
writeOutput( arrayIsDefined( combineArray, 3 ) )

```

Result: false

### Simple example with two dimensional array

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJxzyywqLnEsKkqsVLBViFbg4jTU4eI0AmJjrlhrruLU5Py8FLg0UBYkbQiSNwQrSM7PTcrMS4WpSATRfqnlGgpGCprWXGCuY0FBal6KhgKyUh2FNIS9%2BBUiOwGosrwosyTVv7SkoLREA2KdZ7FLahpQPYZOoBNAOgCe00bJ" target="_blank">Run Example</a>

```java
FirstArray = [ 
	1,
	2,
	3
]
secondArray = [
	11,
	12,
	13
]
combineArray = arrayNew( 2 )
arrayAppend( combineArray, firstArray )
arrayAppend( combineArray, secondArray )
writeOutput( arrayIsDefined( combineArray, 2 ) )

```

Result: true

### Using the indexExists member function

Indexes are one-based, so index 0 never exists and neither does an index past the end of the array.

```java
letters = [ "a", "b" ]
writeOutput( letters.indexExists( 2 ) & "," & letters.indexExists( 3 ) & "," & letters.indexExists( 0 ) )
```

Result: true,false,false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjcEKwjAMhs%2FmKcJOGwwn7Dg8CF58BvGQuVSCa1eyFvHt7apYcsgHf74%2FRqOEkyq98YhXhF1F3s9ctYme8pIMI7k0GRcl9%2FjGNtFSDuE2wBStr5G2ust6ZiOOpxrN%2F0eLPTbYDNh1GDQypG3ZjqxoorsHWdyvozh7KU39Ieuw%2BYbmleEDVd85ZQ%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
]
dump( arrayIsDefined( fruitArray, 3 ) ); // true
// member function
dump( fruitArray.isDefined( 30 ) )
 // false

```


