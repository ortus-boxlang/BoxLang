### Script Syntax

Find the positions of a given string within a provided array regardless of case.


```java
fruitArray = [ 
	"apple",
	"banana",
	"apple",
	"orange",
	"kiwi"
]
applePositions = arrayFindAllNoCase( fruitArray, "APPLE" )
writeOutput( JSONSerialize( applePositions ) )

```

Result: [1,3]

### Case sensitive search with the member function

`findAll` is case sensitive, while `findAllNoCase` ignores case.

```java
fruit = [ "apple", "Apple", "kiwi", "apple" ]
writeOutput( fruit.findAll( "apple" ).toList() & " | " & fruit.findAllNoCase( "apple" ).toList() )
```

Result: 1,4 | 1,2,4

### Find all with a function

When the value is a function, the indexes of the elements it returns true for are returned.

```java
numbers = [ 1, 5, 2, 7 ]
writeOutput( numbers.findAll( ( n ) -> n > 2 ).toList() )
```

Result: 2,4

### No matches

An empty array is returned when nothing matches.

```java
writeOutput( arrayFindAll( [ "a", "b" ], "z" ).len() )
```

Result: 0

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKyrNLHEsKkqsVLBViFbg4lRKLCjISVXSAbKyM8szwYykxDwgBDPzixLz0iHSuUBWPkIhVyxXWmJZflFmSaobyNBioIGJIIPdMvNSHHNyNBTS4HbpKED0KGhypZTmFgClUHVqcino6ytEG8UCANWPMLM%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
]
favoriteFruits = arrayFindAll( fruitArray, "kiwi" )
dump( favoriteFruits )
 // [2]

```


