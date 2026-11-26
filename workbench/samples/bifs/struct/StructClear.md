### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrKMpPy8xJVbBVqFbg4vRz9HVVsFJQ8srPyFPS4eL09AsOCQr1dfULAYmml2aWJBaBxP2dnUMDHEM8%2Ff1A4sWZeempRUpctVzFJUWlySXOOamJRRoKBVCjNbnKizJLUv1LSwpKSzQUvIL9%2FYJTizITczKrUpFUKWgCADymKwE%3D" target="_blank">Run Example</a>

```java
profile = { 
	NAME : "John",
	INSTRUMENT : "guitar",
	OCCUPATION : "singer"
}
structClear( profile )
writeOutput( JSONSerialize( profile ) )

```

Result: An empty struct

### Tag Syntax




```java
<bx:set profile = { 
	NAME : "John",
	INSTRUMENT : "guitar",
	OCCUPATION : "singer"
	} >
<bx:set structClear( profile ) >
<bx:dump var="#profile#"/>
```

Result: An empty struct

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjjsLwkAQhOvcrxiuSiCQXkghJ4iVQgrrNZx6eI%2BwuVNE%2FO%2BaR2Gslm92Z2bJG0e2R40XRKb2R6wgXQiyFNlhtx0oGH%2BT4i2qCs01PNAmZu0jaLKKTXJdDksnbWuplktZ4k5cz4RiCFFWE6OPnNoopjFKORZnY9eslPBf0K6Lz2XdenbQOWpGS9Yaf0HzE1r8v%2FABTNNPTg%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Clear struct
structClear( animals )
// Show animals, now empty
Dump( label="Animals after calling StructClear()", var=animals )

```

Result: the first dump shows COW and PIG; the second dump shows an empty struct


