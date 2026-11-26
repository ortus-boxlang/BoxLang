### New struct using literal notation

Creates an unordered struct.

<a href="https://try.boxlang.io/?code=eJwrzcsvSkktSk1RsFWoVuDidFSwUjDU4eJ0BtLGQNoJSBtx1VpzlRdllqS6lOYWaCiUwvVoWnMBAC8kEWc%3D" target="_blank">Run Example</a>

```java
unordered = { 
	A : 1,
	C : 3,
	B : 2
}
writeDump( unordered )

```

Result: a struct with the keys A, C and B (unordered)


### New struct using function

Creates an unordered struct.

<a href="https://try.boxlang.io/?code=eJwrzcsvSkktSk1RsFUoLikqTS7xSy3X0LTmKoVJ6DkCpQyRBZyAAkbIAs5AAWNrrvKizJJUl9LcAg0FuJwC0CgAniYgjg%3D%3D" target="_blank">Run Example</a>

```java
unordered = structNew()
unordered.A = 1
unordered.B = 2
unordered.C = 3
writeDump( unordered )

```

Result: a struct with the keys A, B and C (unordered)


### New ordered struct using literal notation

Creates an ordered struct. Note the square brackets.

<a href="https://try.boxlang.io/?code=eJzLL0pJLUpNUbBViFbg4nRUsFIw1OHidALSRkDaGUgbc8Vac5UXZZakupTmFmgo5EN1aFpzAQDI2Q9h" target="_blank">Run Example</a>

```java
ordered = [ 
	A : 1,
	B : 2,
	C : 3
]
writeDump( ordered )

```

Result: an ordered struct with the keys A, B, C in that order


### New ordered struct using function

Creates an ordered struct.

<a href="https://try.boxlang.io/?code=eJzLL0pJLUpNUbBVKC4pKk0u8Ust11BQyoeIKiloWnNB2XqOQDWGCK4TkGuE4DoDucbWXOVFmSWpLqW5BRoKUBmQEQB8Th%2BI" target="_blank">Run Example</a>

```java
ordered = structNew( "ordered" )
ordered.A = 1
ordered.B = 2
ordered.C = 3
writeDump( ordered )

```

Result: an ordered struct with the keys A, B, C in that order


### New ordered struct using literal notation

Creates an ordered struct.


```java
ordered = []
ordered.A = 1
ordered.B = 2
ordered.C = 3
writeDump( ordered )

```

Result: an ordered struct with the keys A, B, C in that order


### New case-sensitive struct using function

Creates a case-sensitive struct.

<a href="https://try.boxlang.io/?code=eJxLTixOLU7NK84sySxLVbBVKC4pKk0u8Ust11BQSkaWU1LQtOZCEdFzBKo3RBd0AgoaoQs6AwWNrbnKizJLUl1Kcws0FFDkQUYDAPspLu4%3D" target="_blank">Run Example</a>

```java
casesensitive = structNew( "casesensitive" )
casesensitive.A = 1
casesensitive.B = 2
casesensitive.C = 3
writeDump( casesensitive )

```

Result: a case-sensitive struct with the keys A, B and C


### New ordered and case-sensitive struct using function

Creates a case-sensitive struct.

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVKAYz%2FFLLNRSU8otSUotSU3STE4tTi1PzijNLMstSlRQ0rbmK4Vr0nIGajFFEnIAiRigijkARQ2uu8qLMklSX0twCDQWEJMg4AJpRK88%3D" target="_blank">Run Example</a>

```java
someStruct = structNew( "ordered-casesensitive" )
someStruct.C = 3
someStruct.B = 2
someStruct.A = 1
writeDump( someStruct )

```

Result: an ordered, case-sensitive struct with the keys C, B, A in that order


### Additional Examples


```java
dump( var=structNew( "soft" ), label="soft" )
dump( var=structNew( "weak" ), label="weak" )
dump( var=structNew( "linked" ), label="linked" )
st = {
	"one" : [
		1,
		2,
		3
	],
	"two" : {
		"three" : QueryNew( "id" )
	},
	THREE : "unquoted keys don't preserve case"
}
dump( st )
dump( structKeyList( st ) )
dump( structKeyExists( st, "one" ) )
// shorthand syntax for a new empty ordered struct, [=] also works
st = []
st.C = 1
st.B = 2
st.A = 3
dump( st )
// shorthand syntax for an ordered struct with values
st = [
	C : 1,
	B : 2,
	A : 3
]
dump( st )

```

Result: dumps of the soft, weak and linked structs (all empty), followed by dumps of the other values; the last two dumps show ordered structs with keys C, B, A



```java
<bx:set st = structNew() >

<bx:set st[ "name" ] = "John Doe" >
<bx:set st[ "age" ] = 30 >
<bx:set st[ "city" ] = "New York" >

<bx:dump var="#st#">
```

Result: a dump of the struct with the keys name, age and city

### Sorted struct with a sort type

Passing a sort type creates a sorted struct, so keys come back in sorted order.

```java
s = structNew( "sorted", "text", "asc" )
s.banana = 1
s.apple = 2
s.cherry = 3
writeOutput( structKeyList( s ) )
```

Result: apple,banana,cherry

### Unknown struct type

```java
try {
    structNew( "bogus" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Could not create a struct with a type of [BOGUS] as it is not a known type.
