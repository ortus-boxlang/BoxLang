### Example with simple values

Take an array of numbers and shift the first one off.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWK7UHKBgYlFRYmVwRmZaiQaIraDJVV6UWZLqX1pSUAoUAqrRBADlgBE%2F" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
]
el = arrayShift( arr )
writeOutput( el )

```

Result: 1

### Using a member function

This is the same example as above, but using a member function on the array instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiNNTh4jQCYmOuWK7UHKBgYlGRXnFGZlqJhiZXeVFmSap%2FaUlBaYmGAlBWEwB%2F9A8u" target="_blank">Run Example</a>

```java
arr = [ 
	1,
	2,
	3
]
el = arr.shift()
writeOutput( el )

```

Result: 1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNjzELwjAQhefcr3hkUggU29p26SA46%2BAoHRRS6tCkpAniv%2FcgFhsnh8dxj%2BP7OBPGu3YzWlxBYqdI5JyCU1JHB%2Bdur8vw6P0G5nO5pWMYp9WOLMM5%2BCn4mSG5QqFQoqPROn364knsmVpxak7zQ18fL4akSy2VQq3QsMWsDdIaLZku%2FdPGOTgdm94GJ%2F99iVJbxGGhIcLQvQEfylqu" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
ArrayShift( numbers )
Dump( numbers ) // Outputs [ 2, 3, 4 ]
moreNumbers = [
	5,
	6,
	7,
	8
]
ArrayShift( moreNumbers )
Dump( moreNumbers ) // Outputs [ 6, 7, 8 ]
numbers = [
	"one",
	"two",
	"three",
	"four"
]
ArrayShift( numbers )
Dump( numbers )
 // Outputs [ "two", "three", "four" ]

```


