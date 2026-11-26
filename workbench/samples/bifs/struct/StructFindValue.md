### Find first match for a nested struct value



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJ0VLBSMNLh4nQC0iZA2hlIWwBpFyBtaABkuIIYIBVuYAZXLVcu1AQ938hgV2d%2FP5fgkKBQ5xCQgUDzDIHKTEEaHY0IaAByQzw8g1C0OxnCrHMCaTczBWsPS8wpTQUqgBjjlpmXAhbRUICZrKOgZGikBCTz81KVFDS5wosyS1L9S0sKSks0FLyC%2Ff2CU4syE3Myq8B6IMZpKmgCAFB1Rz4%3D" target="_blank">Run Example</a>

```java
myStruct = { 
	A : 2,
	B : 4,
	C : 8,
	D : 10,
	E : 12,
	F : 12
}
myStruct.MYSECONDSTRUCT = {
	A1 : 50,
	A2 : 12
}
myStruct.MYSECONDSTRUCT.MYTHIRDSTRUCT = {
	B1 : 12,
	B2 : 65
}
myValue = StructFindValue( myStruct, "12", "one" )
WriteOutput( JSONSerialize( myValue ) )

```

Result: [{"path":".E","owner":{"A":2,"B":4,"C":8,"D":10,"E":12,"F":12,"MYSECONDSTRUCT":{"A1":50,"A2":12,"MYTHIRDSTRUCT":{"B2":65,"B1":12}}},"key":"E"}]

### Find all matches for a nested struct value



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJ0VLBSMNLh4nQC0iZA2hlIWwBpFyBtaABkuIIYIBVuYAZXLVcu1AQ938hgV2d%2FP5fgkKBQ5xCQgUDzDIHKTEEaHY0IaAByQzw8g1C0OxnCrHMCaTczBWsPS8wpTQUqgBjjlpmXAhbRUICZrKOgZGikBCQTc3KUFDS5wosyS1L9S0sKSks0FLyC%2Ff2CU4syE3Myq8B6IMZpKmgCAE7LRzU%3D" target="_blank">Run Example</a>

```java
myStruct = { 
	A : 2,
	B : 4,
	C : 8,
	D : 10,
	E : 12,
	F : 12
}
myStruct.MYSECONDSTRUCT = {
	A1 : 50,
	A2 : 12
}
myStruct.MYSECONDSTRUCT.MYTHIRDSTRUCT = {
	B1 : 12,
	B2 : 65
}
myValue = StructFindValue( myStruct, "12", "all" )
WriteOutput( JSONSerialize( myValue ) )

```

Result: [{"path":".E","owner":{"A":2,"B":4,"C":8,"D":10,"E":12,"F":12,"MYSECONDSTRUCT":{"A1":50,"A2":12,"MYTHIRDSTRUCT":{"B2":65,"B1":12}}},"key":"E"},{"path":".F","owner":{"A":2,"B":4,"C":8,"D":10,"E":12,"F":12,"MYSECONDSTRUCT":{"A1":50,"A2":12,"MYTHIRDSTRUCT":{"B2":65,"B1":12}}},"key":"F"},{"path":".MYSECONDSTRUCT.A2","owner":{"A1":50,"A2":12,"MYTHIRDSTRUCT":{"B2":65,"B1":12}},"key":"A2"},{"path":".MYSECONDSTRUCT.MYTHIRDSTRUCT.B1","owner":{"B2":65,"B1":12},"key":"B1"}]

### Find first match for a nested struct value using member function

Calling the findValue member function on a struct.

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJ0VLBSMNLh4nQC0iZA2hlIWwBpFyBtaABkuIIYIBVuYAZXLVcu1AQ938hgV2d%2FP5fgkKBQ5xCQgUDzDIHKTEEaHY0IaAByQzw8g1C0OxnCrHMCaTczBWsPS8wpTQUqgBuUlpmXAhbUUFAyNFLSUVDKz0tVUtDkCi%2FKLEn1Ly0pKC3RUPAK9vcLTi3KTMzJrAKqhJmjqaAJAFK%2FRLs%3D" target="_blank">Run Example</a>

```java
myStruct = { 
	A : 2,
	B : 4,
	C : 8,
	D : 10,
	E : 12,
	F : 12
}
myStruct.MYSECONDSTRUCT = {
	A1 : 50,
	A2 : 12
}
myStruct.MYSECONDSTRUCT.MYTHIRDSTRUCT = {
	B1 : 12,
	B2 : 65
}
myValue = myStruct.findValue( "12", "one" )
WriteOutput( JSONSerialize( myValue ) )

```

Result: [{"path":".E","owner":{"A":2,"B":4,"C":8,"D":10,"E":12,"F":12,"MYSECONDSTRUCT":{"A1":50,"A2":12,"MYTHIRDSTRUCT":{"B2":65,"B1":12}}},"key":"E"}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljztvgzAUhWf8K468BCSk7JUYEG0ihiRVqVqpmwtOatWPCnAzIP57DTiNgO0%2BvnvOuUwLxWSDBB1IkJ3e8YCOBMHxlBdPrqbKGBq7QZF%2FjL1k9YVTEvRu%2BJzvl7gR%2BnvGK14Jq%2FxBlr6u9Lm5zg4al0cOPOnJdoviy1zhJmBTUvJo1U8IyT65TGh6X9AYv6xOfIdoON4JXfk9SqNbJrTQF8dJy2HO2EzpNuTswMNYpxOdoGhrW7aDwtuAhzef%2BP%2BnyWMMWPPGyraB0FhKzQO%2FeNCZLw3u%2BjcDGvmnVvGiPxwnhhY%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : {
		NOISE : "moo",
		SIZE : "large"
	},
	PIG : {
		NOISE : "oink",
		SIZE : "medium"
	},
	CAT : {
		NOISE : "meow",
		SIZE : "small"
	}
}
// Show all animals
Dump( label="All animals", var=animals )
// Find animal containing value of 'medium'
findMediumAnimal = StructFindValue( animals, "medium" )
// Show results in findMediumAnimal
Dump( label="Results of StructFindValue(animals, ""medium"")", var=findMediumAnimal )

```

Result: a dump of all animals, then a dump of an array with one result struct for the PIG entry (path .PIG.SIZE)


