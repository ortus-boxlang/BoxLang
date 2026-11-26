### Traditional function



<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGq5SovyixJdSnNLdBQKC4pKk0uCUvMKU11LCpKrASJIByhqaAJACMFKOk%3D" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
}
writeDump( structValueArray( statusCodes ) )

```

Result: [200, 201, 304, 400, 404] (value order is not guaranteed for a default struct)

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzMvMTcwpVrBVqFbg4nT2D1ewUlDKzc9X0uHiDPB0B%2FHyM%2FOyQVxnxxCwZGp%2BOYjr5BnkAuInZ2QWFShx1XKVF2WWpKaU5hZoKBSXFJUml4Ql5pSmOhYVJVZqKCRC7dFU0AQAFSUhbA%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow",
	BIRD : "chirp"
}
writedump( structValueArray( animals ) )

```

Result: an array of the four values moo, oink, meow and chirp (order not guaranteed for the default struct type)

### Values of an ordered struct

```java
s = [ a : 1, b : 2, c : 3 ]
writeOutput( s.valueArray().toList() )
```

Result: 1,2,3
