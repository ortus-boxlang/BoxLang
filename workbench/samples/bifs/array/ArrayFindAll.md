### Script Syntax

Find the positions of a given string within a provided array regardless of case.


```java
var fruitArray = [ 
	"apple",
	"banana",
	"apple",
	"orange",
	"kiwi"
];
var applePositions = arrayFindAllNoCase( fruitArray, "APPLE" );
writeDump( applePositions );

```

Result: [1,3].

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKyrNLHEsKkqsVLBViFbg4lRKLCjISVXSAbKyM8szwYykxDwgBDPzixLz0iHSuUBWPkIhV6w1V1piWX5RZkmqG8jUYqCJiSCT3TLzUhxzcjQU0uCW6ShANCloWnOllOYWAOVQtQLFFfT1FaKNYrkAj3gxbg%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
];
favoriteFruits = arrayFindAll( fruitArray, "kiwi" );
dump( favoriteFruits );
 // [2]

```


