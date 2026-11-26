### structEach() with an inline function (closure)

Use a function to write out the keys in a structure to the screen

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRkDaGUgbc9VyFYMVuSYmZ2goFMM16ShoKDjmVSpkp1bqgBlliTmlqQqaCrZ2CtVcnOVFmSWp%2FqUlBaUlGgpK3qmVCkoKaiDFQFJJIbMYzIVoAQpYA7maXLUKmgBCnijk" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2,
	C : 3
}
structEach( someStruct, ( Any key, Any value ) => {
	writeOutput( "Key " & key & " is " & value & "; " )
} )

```

Result: Key a is 1; Key b is 2; Key c is 3; 

### Using a function reference



<a href="https://try.boxlang.io/?code=eJydUF1LwzAUfW5%2BxaEPs4XK3oUJou5JqUz%2FwF2XrqFdWpKblm74301jmUOYD76Em3s%2Bck5K6i1WOEFEj%2FlLvsEd4m3jZJyJaJ3nT9O9U8cjTYv3t3zzERhka8lbappYfAqxXELTQe5QOl2wanXYtKwKCa6I%2FSHPGJhqacFDCzJ7d5CabRYYtRxBeoeefAB0pAzaMiCFM8bzoFgaCiYzYNm4gp2RN3aS3%2F5IrTg%2FuJe8pr41Xm0TPOhxomZh%2BOanOIlomPDccec4Qfw6opw1iLEI2RZ%2BUjZcZ50v75sap%2Bcgz1RUSYpBcQX6%2FScXlMnbl74M5s2uW2ko3Sgt%2FzK70mx1%2F%2F9ySL8ABb6xkg%3D%3D" target="_blank">Run Example</a>

```java
favs = { 
	COLOR : "blue",
	FOOD : "pizza",
	SPORT : "basketball"
}

// named function
// notice that the function takes two arguments, the key and value pair of the current iteration of the structure's key-value pairs
function getFavorites( Any key, Any value ) {
	writeOutput( "My favorite " & key & " is " & value )
}
// run structEach() with a named function
structEach( favs, getFavorites )
// run structEach() with an inline function
structEach( favs, ( Any key, Any value ) => {
	writeOutput( "My favorite " & key & " is " & value )
} )

```

Result: prints My favorite <key> is <value> for each entry, twice (once for each approach)


### Using the member function



<a href="https://try.boxlang.io/?code=eJxFjVELgjAUhZ%2Fdr7i4lwlSVj5FCuYmSOTI9FmsBkVR4bZCxP%2Fe3Etv93znO1ypWqVl%2BroICREMgBy%2BgzUsg8BHTlqypGLU5oXJBa%2BaPad5llu4CkIDtwltSnao2bEyLLTDScx4XVBLQjQi%2Bf8zE%2B35SoBA8uzhLnrfHp%2F2oQV4EMUwIOfb3ZTgWr21IuBiY%2BGpwdbCm1MH89gFD43g%2FQBSzjQz" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
}
statusCodes.each( ( Any key, Any value ) => {
	writeOutput( "#key# => #value#<br />" )
} )

```

Result: NOT_FOUND => 404
BAD_REQUEST => 400
CREATED => 201
OK => 200
NOT_MODIFIED => 304

### Accessing a reference to the looping struct in the callback

<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGq5SpG2KOXmpicoaGgoeCYV6mQnVqpA2aUJeaUpkKYxSVFpcklCpoKtnYK1Vyc5UWZJakupbkFGnAZrloFTQDRIjOI" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
}
statusCodes.each( ( Any key, Any value, Any struct ) => {
	writeDump( struct )
} )

```

Result: dumps the full statusCodes struct once for each of its 5 entries


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdzcsKgkAYBeC18xSHWaSC5D4wCJMIFwUGEeFikiHFGSe8JCK%2Be16SsOWZOf93WJZIJgo4aEE093TFBlQqRS2inY%2BHIakkS4fo7i7jJ1c1JR0JyryKSo9FsQE2KRYM7LIGKW9gwtmiJZptI4hVPb7pLH9Wkmdlse6jTrR9JV8GBHtw4VCfN9TCm%2BXOr%2BZ7N5gLRC%2F6iqh4j02j9wUa%2FqlLagU6389TXwTLYtivdjA%2FsIhYig%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
}
StructEach( animals, ( Any key ) => {
	// Show key 'arguments.key'
	Dump( label="Key", var=arguments.KEY )
	// Show key's value 'animals[arguments.key]'
	Dump( label=arguments.KEY & "'s value", var=animals[ arguments.KEY ] )
} )

```

Result: a pair of dumps (key and value) for each of COW, PIG and CAT


