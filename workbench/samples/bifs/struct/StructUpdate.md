### Updates a structure value at specific key

Change value of structure item

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJ0VLBSMNTh4nQC0kZA2hlIGwNpFyBtwlXLVQxWHFqQkliSqqGQC9Wso6CUrKSjYGiqoMlVXpRZkupSmlugoeAV7O8XnFqUmZiTWYWkWkFTQRMASJwhhA%3D%3D" target="_blank">Run Example</a>

```java
myStruct = { 
	A : 1,
	B : 2,
	C : 3,
	D : 4
}
structUpdate( myStruct, "c", 15 )
writeDump( JSONSerialize( myStruct ) )

```

Result: {"A":1,"B":2,"C":15,"D":4} (key C was updated; the key order shown may differ)

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjj0LgzAURWfzKy5vsiC4FxzEQulkwZbOqQYM1UTSpA6l%2F73Gj1Id77vvHC5XsuXNEwneYEGW37AHtVpTxILz6eiTlurhY5ZexlLontiHxTGKWvconTFCWfDJxA6u7UI0%2FC6ahLJ1SRFe3CRzws5Lrl3FrUDJLaT6WQprXGmnLlyuEWh4GyTUDV6a%2BHHEYuylreFGqtoqV8PSf8A%2FztB24Rcuh1zd" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Update cat in animals
StructUpdate( animals, "cat", "purr" )
// Show animals with updated cat in animals
Dump( label="Animals with cat updated", var=animals )

```

Result: the first dump shows cat as meow; the second dump shows cat as purr

### Set a key that does not exist yet

Unlike structInsert, structUpdate never throws for an existing key, and it also creates missing keys.

```java
s = { a : 1 }
structUpdate( s, "b", 2 )
writeOutput( s.b )
```

Result: 2
