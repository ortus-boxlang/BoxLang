### Using structCount as a function

Pass a structure as an argument to the function.

<a href="https://try.boxlang.io/?code=eJwrLU4tUrBVqFbg4vR0UbBSMNTh4nTzDAoO8XP0dQXylTwS85SAYj6OCKHg%2FJx8kJivf5gnRKAksUghPLGoWImr1pqrvCizJNWlNLdAQ6G4pKg0ucQ5vzSvREOhFGSVpoKmNRcA4RsgBA%3D%3D" target="_blank">Run Example</a>

```java
user = { 
	ID : 1,
	FIRSTNAME : "Han",
	LASTNAME : "Solo",
	MOVIE : "Star Wars"
}
writeDump( structCount( user ) )

```

Result: 4

### Using structCount as a Member Function

 Having a reference of a structure as a variable, use the dot notation to get a key count on it using a member function.

<a href="https://try.boxlang.io/?code=eJwrLU4tUrBVqFbg4vR0UbBSMNTh4nTzDAoO8XP0dQXylTwS85SAYj6OCKHg%2FJx8kJivf5gnRKAksUghPLGoWImr1pqrvCizJNWlNLdAQ6EUaLpecn5pXomGpoKmNRcA%2FjQdbQ%3D%3D" target="_blank">Run Example</a>

```java
user = { 
	ID : 1,
	FIRSTNAME : "Han",
	LASTNAME : "Solo",
	MOVIE : "Star Wars"
}
writeDump( user.count() )

```

Result: 4

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzMvMTcwpVrBVqFbg4nT2D1ewUlDKzc9X0uHiDPB0B%2FHyM%2FOylbhqrbkSwWqd80vzSoDqIbxivWQQX0PTmis1OSNfQ0EpJCO1KFUhEYiVFNQUkPWoAUUgfI1iTYXMPIWSjFQFdag56grFJUWlySVKCkCjACgrLaE%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
animalCount = animals.count()
echo( "There are " & animalCount & " animal(s) in the 'animals' struct" )

```


<a href="https://try.boxlang.io/?code=eJxLzMvMTcwpVrBVqFbg4nT2D1ewUlDKzc9X0uHiDPB0B%2FHyM%2FOylbhqrbkSwWqd80vzSoDqg0uKSpNLwDwNhUSoMZrWXKnJGfkaCkohGalFqQqJQKykoKaArFUNKALhaxRrKmTmKZRkpCqoQ01QVygGm6sEMgoAisowGA%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
animalCount = StructCount( animals )
echo( "There are " & animalCount & " animal(s) in the 'animals' struct" )

```


### Length of a string, array and set

```java
writeOutput( len( "hello" ) & " " & len( [ 1, 2, 3 ] ) & " " & len( setOf( 1, 2, 2, 3 ) ) )
```

Result: 5 3 3

### Length of a query using its alias

```java
q = queryNew( "id", "integer", [ [ 1 ], [ 2 ] ] )
writeOutput( queryRecordCount( q ) & " " & len( q ) )
```

Result: 2 2

### Use the string member function

```java
writeOutput( "hello".len() )
```

Result: 5

### A null has a length of zero and unmeasurable objects throw

```java
writeOutput( len( nullValue() ) )
try {
	len( new java.lang.Object() )
} catch( any e ) {
	writeOutput( " " & e.message )
}
```

Result: 0 Cannot determine length of object of type Object
