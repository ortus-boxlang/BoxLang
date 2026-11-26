### Remove a key from a struct

Creates a struct then removes a key

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRly1XMVgSZfUnNSSVA2FYrhyHQWlRCUFTa7yosySVJfS3AJkSQVNAI2zG1c%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2
}
structDelete( someStruct, "a" )
writeDump( someStruct )

```

Result: a struct with one key-value pair: B = 2

### Remove a key from a struct using the member function

 Invoking the delete function on a struct is the same as running structDelete.

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4nRUsFIw1OHidALSRly1XMVwBXopqTmpJakaCkqJSgqaXOVFmSWpLqW5BRoKCDUKmgDK%2Bhi0" target="_blank">Run Example</a>

```java
someStruct = { 
	A : 1,
	B : 2
}
someStruct.delete( "a" )
writeDump( someStruct )

```

Result: a struct with one key-value pair: B = 2

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjkELgkAQhc%2Fur3jsRQPBe%2BBBFKJTgUHnVSdWXN1Y1ySi%2F56uFthpePO%2BefNEV7dC9YjxAvPS0xV78FZrHjLvfDzMStddM8s0uTiT9MjZm0URcqlHlIMx1FmIJYllQ3sPoERBKubp1uQhHsLEq8JuDslIkSVYSWjoCb8U1sfN6Ba9NUNpWe7GggXfpBB8AvkS4Xr8jGkPKXoURB0qd1ZtWyXr%2F7G20uEr9V%2FvA%2FmhW2M%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Delete the key 'cat' from struct
StructDelete( animals, "cat" )
// Show animals, cat has been deleted
Dump( label="Animals with cat deleted", var=animals )

```

Result: the first dump shows COW, PIG and CAT; the second dump shows only COW and PIG

### Deleting a key that does not exist

Removing a missing key is not an error; the struct is returned unchanged.

```java
s = { a : 1 }
result = structDelete( s, "missing" )
writeOutput( structCount( result ) )
```

Result: 1
