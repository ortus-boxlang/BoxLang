### Simple example for arraySort function

Uses the arraySort() function to get the sorted array and which sorted by type numeric

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDTQ4eI0AhG6lpZA0sQMSJgacMVyJYJUBecXlWgoFMM06Sgo5ZXmphZlJisBmSmpxclKCppc5UWZJan%2BpSUFpUC1XsH%2BfsFAFYk5mVWpSFoVNBU0AVHhJJc%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	10,
	20,
	-99,
	46,
	50
]
arraySort( someArray, "numeric", "desc" )
writeOutput( JSONSerialize( someArray ) )

```

Result: [50,46,20,10,-99]

### Simple example with member function



<a href="https://try.boxlang.io?code=eJwrzs9NdSwqSqxUsFWIVuDiVHLyj%2FBx9HNX0gGyk%2FIrchLz0sHsxJT8pFQwywlJNMjR08dfiSvWmqsYZpBecX5RiYaCUklqRYmSjoJSSmpxspKCpjVXeVFmSap%2FaUlBKVDaK9jfLzi1KDMxJ7MqVUMBrltBE6QUAJoQLF0%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"BOXLANG",
	"boxlang",
	"adobe",
	"Boxlang",
	"RAILO"
]
someArray.sort( "text", "desc" )
writeOutput( JSONSerialize( someArray ) )

```

Result: ["boxlang","adobe","RAILO","Boxlang","BOXLANG"]

### Simple example with callback function

Uses the callback function

<a href="https://try.boxlang.io/?code=eJxVjbEKgzAQhufkKX6cFEKhFjoULGSQQqE6OJYOodwgqJEzUmzx3ZtYhDocd9x993%2BDbUkzmwkZ7pDiI4Uo9C3HCZGjwVHbN3YiipQ%2F6MuyP6SRFLPawisYnjbwMcDyIU2wVJZdjGGVKsTQ3QTaq19PkSA7wwczuZE7PG3bG6bYI7ugUp5ZBiRy9vXi2lE5un70udeqLCri2jT1m%2F40PjT5ArlmQbw%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	{
		NAME : "testemployee",
		AGE : "32"
	},
	{
		NAME : "employeetest",
		AGE : "36"
	}
]
arraySort( someArray, ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME )
} )
writeOutput( JSONSerialize( someArray ) )

```

Result: [{"NAME":"employeetest","AGE":"36"},{"NAME":"testemployee","AGE":"32"}] 

### Script member syntax: sort array of structs by multiple keys

Takes an array of structs and sorts by multiple different keys, similar to the way a query allows. Active employees come first, ordered by department and then by name. The last names are output in the sorted order.

<a href="https://try.boxlang.io/?code=eJyVkl1vgjAUhq%2FhV5xxBYkScdvNEpagssTFj0XMbpZddFhnM6CmLRoz%2Fe9rQVRYl8yLJu055z3PedsixtBuuowEy2PBwYc3MI1v0zCsnGM2XFjwAF5LnZeEcTFBKVYh65muMquIJ%2BgiPKC4jC7wGjGR4uyci1CCeZlFsSCbIuqZxqH1i9jVEVGGNcQoJWL1J3OM2BcWJPv8H%2FdWww0SEuvA6gY4za6z29Fi7zTYHv3QQHuMbq9E6p3ea5D9FWIJ0XkdoA3h11%2Bycmu%2Bm6j2x1xOmbDBhiDbgRrIa522XXDAfwQ5K1naZdIN%2BvPhawg3fllRnR1VZTAscpbVM%2B2aUI1w0W0QvgSz%2BTiczCfBOAS%2F6tqI7%2Fd1uqzrHJHnXqMganQ5RcrSy%2FE892k4K7NuTFN5idg%2Bqk4JcKToYDZ0VdOm7AwrPNY0dTtNZcOsYx7k2jIi8DQX61y%2BTuPJUrRWL6bU0lr7sdi51RcBxxV0RLiwHXB%2BAAD1N24%3D" target="_blank">Run Example</a>

```java
arrayOfStructs = [ 
	{
		"userId" : 1,
		"firstName" : "John",
		"lastName" : "Doe",
		"departmentName" : "Sales",
		"active" : 1
	},
	{
		"userId" : 2,
		"firstName" : "Jane",
		"lastName" : "Smith",
		"departmentName" : "Marketing",
		"active" : 1
	},
	{
		"userId" : 3,
		"firstName" : "Alice",
		"lastName" : "Johnson",
		"departmentName" : "Sales",
		"active" : 0
	},
	{
		"userId" : 4,
		"firstName" : "Bob",
		"lastName" : "Brown",
		"departmentName" : "Sales",
		"active" : 1
	},
	{
		"userId" : 5,
		"firstName" : "Charlie",
		"lastName" : "Davis",
		"departmentName" : "Marketing",
		"active" : 0
	}
]
arrayOfStructs.sort( ( Any user1, Any user2 ) => {
	if( user1.ACTIVE != user2.ACTIVE ) {
		return user2.ACTIVE - user1.ACTIVE
	}
	if( user1.DEPARTMENTNAME == user2.DEPARTMENTNAME || user1.ACTIVE == 0 ) {
		if( user1.LASTNAME == user2.LASTNAME ) {
			return user1.FIRSTNAME.compare( user2.FIRSTNAME )
		}
		return user1.LASTNAME.compare( user2.LASTNAME )
	}
	return user1.DEPARTMENTNAME.compare( user2.DEPARTMENTNAME )
} )
writeOutput( arrayOfStructs.map( ( user ) -> user.lastName ).toList() )

```

Result: Smith,Brown,Doe,Davis,Johnson

### Default sort is a case insensitive text sort

```java
names = [ "bob", "Alice", "carol" ]
names.sort()
writeOutput( names.toList() )
```

Result: Alice,bob,carol

### Invalid sort order

Only "asc" and "desc" are accepted as the sort order when no callback is used.

```java
try {
	arraySort( [ 3, 1, 2 ], "numeric", "sideways" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: You must supply either a sortOrder or callback

### Additional Examples

<a href="https://try.boxlang.io/?code=eJytkEGLwjAQhc%2FJrxhyqhCUVdiDoBBBvLh66GVh8ZC2467QJCVN0SL%2B900ilYoIHjzNY%2FLNy7xJjUJhrWxhBj9ACVtsv9dis2Lc68ycSql%2Fo5aFyTCqRa%2Fb0XRHZXBJjXUJpJ0pB%2Bbw5JivBdY5gwEtGlX1CN8ZjUChytDCvtG5OxhNb8%2FDOhq%2B4JL2g1BypoRsxNcSpmG4dqiq0rQYExCxiv3JmFFy4fdwB4ahO%2FgzwE9zJiB0C%2FjBr3UMA5jNwRtbdI3VkBtVSYuJR4bhK%2B6ZKPzqlxfPAseD%2B4O8NHVj8eFGb9%2FgH7gonUw%3D" target="_blank">Run Example</a>

```java
SomeArray = [ 
	"BOXLANG",
	"boxlang",
	"adobe",
	"Boxlang",
	"BOXLANG"
]
arraySort( SomeArray, "text", "desc" )
dump( SomeArray )
// member function
SomeArray.sort( "text", "desc" )
dump( SomeArray )
SomeArray = [
	{
		NAME : "testemployee",
		AGE : "32"
	},
	{
		NAME : "employeetest",
		AGE : "36"
	}
]
arraySort( SomeArray, ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME )
} )
dump( SomeArray )
// member function with closure
SomeArray.sort( ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME )
} )
dump( SomeArray )

```


