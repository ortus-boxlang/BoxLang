### structEach() with an inline function (closure)

Use a function to write out the keys in a structure to the screen

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRkDaGUgbc9VacxWDVbkmJmdoKBTDdekoaCg45lUqZKdW6oAZZYk5pakKmgq2dgrVXJzlRZklqf6lJQWlJRoKSt6plQpKCmogxUBSSSGzGMyFaAEKWAO5mtZctSACAOHWKZ8%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2,
	C : 3
};
structEach( someStruct, ( Any key, Any value ) => {
	writeOutput( "Key " & key & " is " & value & "; " );
} );

```

Result: Key a is 1; Key b is 2; Key c is 3; 

### Using a function reference



<a href="https://try.boxlang.io/?code=eJylUE1rwkAQPWd%2FxSMHq5DivWKhtPXUkmL7B8a4MUviJuzOJkTxvze7DVYK9uJlmJn3sW82p9ZiiSNE9Jy%2BpWs8IN5UTsaJiFZp%2BuLnRh0O5BefH%2Bn6KzDIlpI3VFWxOC2EmM%2BhaS%2B3yJ3OWNU6bGpWmQQXxEORZwxMpbTgrgaZndtLzTYJjFL2IL1FS0MCNKQM6jwgmTNm4EGxNBRMRsCycRk7I%2B%2Bsl9%2F%2FSq04P7iTvKK2NoPaTvGke09NQvPDn%2BEoos7jqePG8RTxe4981CDGJGSbDJ2yYRx1C3HypxqnxySvlBXTGTrFBejvp1xQvPlw9WUy73bdS0PpSmn5n9uV25aPN5znyzfgvrLD" target="_blank">Run Example</a>

```java
favs = { 
	COLOR : "blue",
	FOOD : "pizza",
	SPORT : "basketball"
};

// named function
// notice that the function takes two arguments, the key and value pair of the current iteration of the structure's key-value pairs
function getFavorites( Any key, Any value ) {
	writeOutput( "My favorite " & key & " is " & value );
}
// run structEach() with a named function
structEach( favs, getFavorites );
// run structEach() with an inline function
structEach( favs, ( Any key, Any value ) => {
	writeOutput( "My favorite " & key & " is " & value );
} );

```


### Using the member function



<a href="https://try.boxlang.io/?code=eJxFjcEKgkAQhs%2FuUwzuRUHKylOmYO4KErlkeharhaKocHcLEd%2B9dS9dhvm%2F%2BYZfyFYqkb4uXEAEAyCL7WANS9%2F3kJWWNKkoMXmhc8GqZs9InuUGrvxAw21CmpIeanqsNAvM4yRmrC6IIQEaQyT%2BRTPenq8OOJA8e7jz3jPLp30oDi5EMQzI%2BnY3yZmSbyUdsLG28HTBxsKbUwfz2AY3ROM0fh9xNO4%3D" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
};
statusCodes.each( ( Any key, Any value ) => {
	writeOutput( "#key# => #value#<br />" );
} );

```

Result: NOT_FOUND => 404
BAD_REQUEST => 400
CREATED => 201
OK => 200
NOT_MODIFIED => 304

### Accessing a reference to the looping struct in the callback

<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGqteYqRlikl5qYnKGhoKHgmFepkJ1aqQNmlCXmlKZCmMUlRaXJJQqaCrZ2CtVcnOVFmSWpLqW5BRpwGWuuWhABAJnsNEM%3D" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
};
statusCodes.each( ( Any key, Any value, Any struct ) => {
	writeDump( struct );
} );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjUsLgkAUhdfOrzjMIhUk94mBmES4KDCIiBaTDCk%2BJtRJRPzv%2BUjCNhe%2Be8%2F9DsvjjKUlbLQginu8YAOaCUENopwO%2B4FEnCcDus55PHJRU9JZJKgKGVYeCyMNbNIY0ODkDRLeQIe9RUsU00QQiXrcqax4yoznVbnuUSXKTmYvDSl78NSmPm%2BogTcr7F%2FM967QrYVFLftMKnlvm1pvC%2Bv9T7t0rUDn%2F7nrK8EyeB9qu2F8AE2dWYA%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
};
StructEach( animals, ( Any key ) => {
	// Show key 'arguments.key'
	Dump( label="Key", var=arguments.KEY );
	// Show key's value 'animals[arguments.key]'
	Dump( label=arguments.KEY & "'s value", var=animals[ arguments.KEY ] );
} );

```


