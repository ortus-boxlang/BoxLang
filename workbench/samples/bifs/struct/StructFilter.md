### Example using a simple numeric comparison

Take a struct of items with their rating and use structFilter to return ones of a rating 3 and higher.

<a href="https://try.boxlang.io/?code=eJxVjUELgkAQhc%2FOr5ijgpeyLonCBhlSqXjpLLnGklmss4aI%2F73Z6FDM5Xvz4HuNNorKilR37THCCcERRXHc4QZXPjhbkfFxWHDIS5HtbbPmcGLOmZfMh%2FScMgYwh9BUw0MrkokVW2VP2lwoUS1J7WLzs%2Beji6Ib8SZH%2FwND1RqJHkYxTuBoSUZ332ccYRDCjF4IL6uvzf3Jtv8xLt%2FF2j2S" target="_blank">Run Example</a>

```java
fruitRatings = { 
	APPLE : 4,
	BANANA : 1,
	ORANGE : 5,
	MANGO : 2,
	KIWI : 3
};
favoriteFruits = structFilter( fruitRatings, ( Any key, Any value ) => {
	return value >= 3;
} );
writedump( favoriteFruits );

```

Result: {apple=4,orange=5,kiwi=3}

### Example using a member function

This is the same example, but using a member function on the struct instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxdjcEKgkAURdfOV9ylggRlbRKFCSqkUnHTWnAmhsximjFE%2FPfeRIuItzn3XThXaqtMVRvVXZ5IMIJ5vCyPW6yxDJm34TkdhTmFouL53jUrCifignhBfMjOGWHEppjJur9rZcTOiZ1S%2FizMpGqN0D588G7AVQzhB%2Fq6tQIBkhQj87QwVnffZ5ogitmEIGYv523s7eHjb4XKN%2FmnOu8%3D" target="_blank">Run Example</a>

```java
fruitRatings = { 
	APPLE : 4,
	BANANA : 1,
	ORANGE : 5,
	MANGO : 2,
	KIWI : 3
};
favoriteFruits = fruitRatings.filter( ( Any key, Any value ) => {
	return value >= 3;
} );
writedump( favoriteFruits );

```

Result: {apple=4,orange=5,kiwi=3}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kE9LxDAQxc%2FJp3j21EJx7y4Vin%2BW4rIqPYiIhyhTG5qmmqZKWfrdTeLuag8e9zIwb3i%2FmXlCy1aoHhm24Ozi9gFniNqui1LO7oqV7zqpG9%2BWm7xYeyHi05IvFijr7gtCKYgfCL8c2vcYSryQyqL8dxCl%2BBQm23VIgntFdj%2BHrYVFKxqC7mRP3NcxP5xWWjO82mupLJl4b0oRI9cjGhqRIDvHljOHLSpHo6DWoodwm9VAMGQHo%2BFAhDjgd5yEM1nFWJM%2BkJ8gzNvQkrb96c3VI54dP%2FF49oey5Gzie6FyLqdM%2FrVZCJuwKZ%2FFMHvunyyEIXwMkiwP9XhRBPw8ipMjZXEfVs2zmH3nDN8nsNCR" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	SNAIL : ""
};
// Show all animals
Dump( label="All animals", var=animals );
// Get animals that make noise
noisyAnimals = StructFilter( animals, ( Any key ) => {
	// If the key has a value return true (noisy animal)
	if( Len( animals[ arguments.KEY ] ) ) {
		return true;
	}
	return false;
} );
Dump( label="Noisy Animals", var=noisyAnimals );
// Get animals that are quiet
quietAnimals = StructFilter( animals, ( Any key ) => {
	// If the key has a value return true (quiet animal)
	if( !Len( animals[ arguments.KEY ] ) ) {
		return true;
	}
	return false;
} );
Dump( label="Quiet Animals", var=quietAnimals );

```


