### Copy a structure and change it. Original structure stays unchanged



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJUSlRSsFIw1AGykkAsIxArGcQy5qrlyq30Sy2Hqy8GM5zzCyo1FHJhxmgiK9JzAiozRRaJVlBKUVKIBQqbcEH0u6TmpJakgkyAK9JRAFmpyVVelFmS6l9aUlBaogG1zTu10iezuATZQgU1BSWFR22TgKQapiqEgzUVNAFCTUtN" target="_blank">Run Example</a>

```java
myStruct = { 
	"a" : 1,
	"b" : 2,
	"c" : 3
}
myNewStruct = structCopy( myStruct )
myNewStruct.B = 5
myNewStruct[ "d" ] = 4
structDelete( myNewStruct, "c" )
writeOutput( structKeyList( myStruct ) & " → " & structKeyList( myNewStruct ) )

```

Result: b,a,c → b,a,d

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtkMFqwzAQRM%2FRVww%2BJWCSe8EH49LQUwsp5KxYMhaWtUaWY0rov1eSXWPTXASrnX07O9yoluseGR5gu%2BLjihckLVGSst3n%2BzlUpEyTsB92OuFS04hysFYaBz6Nsteh7fbQ%2FCZ1lhTbZpLizm02VzgESEHd918fvbND6eAIFbctC4%2F3com%2FQbjHZjYaCKIUmqjpoVUjnzt5CyheOWnXuMPsKC6KyFwIDyBXe%2BEEOuKqtIb%2F8%2FOV9PaC%2BsgmTN510ojFV4qHzy3%2FirlJGn1SK6uLyPhCmVIPQvYoudt6zecbR%2BXq0AUXQopn4a0CEORRwWTN7%2FI%2FM95fUqekQGWpXYK8yYpsHMDI%2B82qKZVfqd%2Bu8A%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Copy animals struct to farm
farm = StructCopy( animals )
// Show farm, looks like animals
Dump( label="Farm after StructCopy()", var=farm )
// Add another animal. Will not affect farm.
StructAppend( animals, {
	CAT : "meow"
} )
// Show animals, now includes cat
Dump( label="Animals with cat added", var=animals )
// Show farm, does not have cat
Dump( label="Farm copied from animals before cat was added", var=farm )

```

Result: the dumps show farm as COW and PIG, animals gaining CAT after the append, and farm remaining without CAT

### Nested structs are shared by reference

The copy is shallow, so a nested struct is the same object in both structs.

```java
original = { inner : { x : 1 } }
duplicate = structCopy( original )
duplicate.inner.x = 2
writeOutput( original.inner.x )
```

Result: 2
