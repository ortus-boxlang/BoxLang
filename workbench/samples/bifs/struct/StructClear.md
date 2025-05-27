### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrKMpPy8xJVbBVqFbg4vRz9HVVsFJQ8srPyFPS4eL09AsOCQr1dfULAYmml2aWJBaBxP2dnUMDHEM8%2Ff1A4sWZeempRUpctdZcxSVFpcklzjmpiUUaCgVQszWtucqLMktS%2FUtLCkpLNBS8gv39glOLMhNzMqtSkZSBFAIA3vYrvA%3D%3D" target="_blank">Run Example</a>

```java
profile = { 
	NAME : "John",
	INSTRUMENT : "guitar",
	OCCUPATION : "singer"
};
structClear( profile );
writeOutput( JSONSerialize( profile ) );

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

<a href="https://try.boxlang.io/?code=eJxljs0KwjAQhM%2FNUww5tRDoXelBIognhR48ryVqMD8lTRUR311NezB4Wr7ZnZklpy2ZAQ2eYIXcHbAAt95zwYr9dvMlr92Vs9eS1TXai7%2BjG0NQLoImL1uPti9h6KhMw2W%2B5AI3Cs1MqFKKNIoChhjGLrJpJKlEfpfaZknAfUDZPj7ywtVsoVNUAR0Zo90Z7U9q9f%2FEG64LUEQ%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
};
// Show current animals
Dump( label="Current animals", var=animals );
// Clear struct
structClear( animals );
// Show animals, now empty
Dump( label="Animals after calling StructClear()", var=animals );

```


