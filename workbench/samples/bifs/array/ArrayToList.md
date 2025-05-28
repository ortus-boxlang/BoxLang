### Retrieve an array as a list

Uses the arrayToList function with a pipe delimiter to retrieve an array as a list

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNuGKteYqBirwySwuAcongtSF5IN4GgrFMI06Cko1Sgqa1lzlRZklqf6lJQWlUGmwNqAEAIeLHbQ%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4
];
someList = arrayToList( someArray, "|" );
writeOutput( someList );

```

Result: "1|2|3|4"

### Retrieve an array as a list using the Array member function

 Uses the Array member function to retrieve an array as a list

<a href="https://try.boxlang.io/?code=eJwrTs3MS0vNSXEsKkqsVLBViFbg4lTySi0qqlTSAbJccxIz81LBTO%2BixNzUIjDTPTW%2FKD1ViSvWmqsYqt8ns7gEqL0Y2Ti9knyQsIamNVd5UWZJqn9pSUFpiYYCih6gJACWdipK" target="_blank">Run Example</a>

```java
seinfeldArray = [ 
	"Jerry",
	"Elaine",
	"Kramer",
	"George"
];
seinfeldList = seinfeldArray.toList();
writeOutput( seinfeldList );

```

Result: "Jerry,Elaine,Kramer,George"

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVYhW4OJUSlTSAZJJYDIZiZ2CxE4Fk2lKXLHWXCmluQUaCokgI0LyfTKLSzQU8mBGaipoWnPp6yvkpuYmpRYppJXmJZdk5ufpKJRnlmQoJJcWl%2BTnKhSnFiQWJZbkF0GNgunWK4Eap6SrqwQxCgC31S%2FC" target="_blank">Run Example</a>

```java
newArray = [ 
	"a",
	"b",
	"c",
	"b",
	"d",
	"b",
	"e",
	"f"
];
dump( arrayToList( newArray ) );
// member function, with custom separator
dump( newArray.toList( "--" ) );

```


