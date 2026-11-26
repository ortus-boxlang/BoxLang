### Numeric sorting



<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4gxydVGwUrA01uHijHT18fEPB%2FEMgDz3IFdXPxDHhKuWqyi1uDQHpKUYrDc4v6hEQ6EYbpaOglJeaW5qUWayEpCZklqcrKSgyVVelFmS6l9aUlAKVJyTnFicqqHgFezvFwxUmJiTWQXkQs3VBEEAy1MvMw%3D%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	RED : 93,
	YELLOW : 90,
	GREEN : 94
}
result = structSort( someStruct, "numeric", "desc" )
writeOutput( lcase( JSONSerialize( result ) ) )

```

Result: ["green","red","yellow"]

### Sort by subelement



<a href="https://try.boxlang.io/?code=eJxljssKwjAQRdfmK4asWhBBBUHBRdFSEG2L6Q%2BEOGCgL5IpvvDfTVR8MovhXubcubapUJDpFMEcLldmX3ogFllReJf1oiSGGYwmfdZbxnm0LTZxWjiHJ1ijkSVnX2SyjtIvcPoHqqZqO0LzS8bZ1iEf7Hj4x%2Bb7k9Xq8dWg7Urf3d4DRGMogHdgHzjhkepGSYvcKWmVXztspaEKa%2BIQsoPRhFlHrlEApT8NYCWyVKDRstRnJ59%2FQj83SmleAQ%3D%3D" target="_blank">Run Example</a>

```java
someStruct = {}
someStruct.SCOTT = {
	AGE : 26,
	DEPARTMENT : "General"
}
someStruct.GLAN = {
	AGE : 29,
	DEPARTMENT : "computer"
}
someStruct.GEORGE = {
	AGE : 31,
	DEPARTMENT : "Physical"
}
result = structSort( someStruct, "textnocase", "asc", "department" )
writeOutput( lcase( JSONSerialize( result ) ) )

```

Result: ["glan","scott","george"]

### Sort with a callback

The callback receives two key names, so a closure is used to look up the values in the outer struct.

```java
birthdays = { Jim : "1982/12/5", Anne : "1968/9/13", Thomas : "1975/3/28" }
sorted = structSort( birthdays, ( k1, k2 ) => dateCompare( birthdays[ k1 ], birthdays[ k2 ] ) )
writeOutput( sorted.toList( " " ) )
```

Result: Anne Thomas Jim

### Sort by a nested value

With a path, the sort uses the named value inside each nested struct.

```java
animals = { cow : { total : 12 }, pig : { total : 5 }, cat : { total : 3 } }
writeOutput( structSort( animals, "numeric", "asc", "total" ).toList() )
```

Result: cat,pig,cow

### Invalid sort directive

```java
try {
    structSort( { a : 1 }, "bogus", "asc" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: The sort directive [bogus,asc] is not a valid struct sorting directive
