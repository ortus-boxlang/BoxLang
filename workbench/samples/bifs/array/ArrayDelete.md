### Delete an element from an array

Deletes the first `apple` element from the array `arr`.

<a href="https://try.boxlang.io/?code=eJwtyjEKgDAQRNHanGJIZcArWIhWFqawFIstFgkEXdYE0dMraveY%2BaSKGhNMYUkksq0ebUrr8lGY9EXzvmY2pEpnx5ETD1tLO5d4pgp%2FAWcODYl9TpJTiX70w8gaKIbrS%2BHgbgDFI8k%3D" target="_blank">Run Example</a>

```java
arr = [ 
	"apple",
	"orange",
	"pear",
	"Apple"
]
arrayDeleteNoCase( arr, "Apple" )
writeOutput( JSONSerialize( arr ) )

```

Result: ["orange","pear","Apple"]

### Case sensitive delete

`arrayDelete` and the `delete` member function match case sensitively, so only the exact `apple` value is removed.

```java
fruits = [ "apple", "Apple", "pear" ]
fruits.delete( "apple" )
writeOutput( fruits.toList() )
```

Result: Apple,pear

### Delete all matching values

Pass `"all"` as the scope to remove every match instead of only the first.

```java
letters = [ "a", "b", "a", "A" ]
arrayDelete( letters, "a", "all" )
writeOutput( letters.toList() )
```

Result: b,A

### Delete ignoring case using the member function

```java
letters = [ "a", "A", "b" ]
letters.deleteNoCase( "a", "all" )
writeOutput( letters.toList() )
```

Result: b

### Value not found

If the value does not exist the array is left unchanged.

```java
numbers = [ 1, 2, 3 ]
arrayDelete( numbers, 99 )
writeOutput( numbers.len() )
```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io?code=eJyNjr8KwjAQxmfzFEemCoU%2BQMlQ7eLSJTiJw0WuUoi2XFOrb29yirhUXL77w3ff%2FZAZDBxArbTr7x6vZ53H3u7tTqtjqWbuAtXTZcjghmyQOQePjrzRG2p7Jg3rUsU1PmryFKjptzhSBuLU42Q7cfzIqdpAnExQFPCGSIkNzQkt0qAwVU7K6TWI4gJjPP0fU8zpRxTvF2i%2FEz%2FAT7cRXw8%3D" target="_blank">Run Example</a>

```java
arr = [ 
	"boxlang",
	"SUSI"
]
writeDump( var=arr, label="Before" )
arrayDeleteNoCase( arr, "suSi" )
writeDump( var=arr, label="After" ) // boxlang
arrNew = [
	"a",
	"Ab",
	"c",
	"A",
	"a"
]
writeDump( var=arrNew, label="Before" )
arrayDeleteNoCase( arrNew, "a", "all" )
writeDump( var=arrNew, label="After" )

```


