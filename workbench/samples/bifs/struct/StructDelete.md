### Remove a key from a struct

Creates a struct then removes a key

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRly11lzFYFmX1JzUklQNhWK4eh0FpUQlBU1rrvKizJJUl9LcAmRZkAQA87UcEg%3D%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2
};
structDelete( someStruct, "a" );
writeDump( someStruct );

```

Result: Struct with one key-value pair: B 2

### Remove a key from a struct using the member function

 Invoking the delete function on a struct is the same as running structDelete.

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRly11lzFcBV6Kak5qSWpGgpKiUoKmtZc5UWZJakupbkFGgoIRSAJACeFGW8%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2
};
someStruct.delete( "a" );
writeDump( someStruct );

```

Result: Struct with one key-value pair: B 2

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljsEKgkAURdfNV1xmo4HgvnAhCtGqwKD1qC8URyfGMYno39NxCqTV43LuuxzR1a2QPSK8wDbJ6YodeKsUD9jmfDzMSdVdM8ckvlhIauTsvWdhiKxSI4pBa%2BoMxDLF0qG9%2B5AiJxnxZA15gIfQkUvY2pWUJBmCqQgNPeEVwni4adWiN3ooDMvsWWr%2BdyoAn4rcbViTH5kAKtEjJ%2BpQ2r9y7RU7g7E2la271r%2FgB5NlXFk%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
};
// Show current animals
Dump( label="Current animals", var=animals );
// Delete the key 'cat' from struct
StructDelete( animals, "cat" );
// Show animals, cat has been deleted
Dump( label="Animals with cat deleted", var=animals );

```


