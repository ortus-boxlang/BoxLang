### Numeric sorting



<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4gxydVGwUrA01uHijHT18fEPB%2FEMgDz3IFdXPxDHhKvWmqsotbg0B6SnGKw5OL%2BoREOhGG6YjoJSXmlualFmshKQmZJanKykoGnNVV6UWZLqX1pSUApUnZOcWJyqoeAV7O8XDFSZmJNZBeRCDdYEQWsuAHqvL%2B4%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	RED : 93,
	YELLOW : 90,
	GREEN : 94
};
result = structSort( someStruct, "numeric", "desc" );
writeOutput( lcase( JSONSerialize( result ) ) );

```

Result: ["green", "red", "yellow"]

### Sort by subelement



<a href="https://try.boxlang.io/?code=eJxljs0KwjAQhM%2FNUyw5VSiCCoIWD0VLQbQV0xcIccFCfyTZ4h%2B%2Bu4kUtZQ9LDvsNzOmqVCQbhXBCp6vkJmvMBbrLM%2BdzLwoiWEJ03nAvE18iI75Pk5zq%2FAEa9Sy5KyPJrso7ZGLAama6tIS6gEaZ0fL%2FMGzyQA%2BnO%2BmUF2uRtOWrr%2F5OIhGkw8%2FxwA44Y3qRkmD3F7SKLdOeJGaKqyJwyhkV10QZi3ZUj6U7teHrchSgbqQZfGwZxc0chOyN5rWX20%3D" target="_blank">Run Example</a>

```java
someStruct = {};
someStruct.SCOTT = {
	AGE : 26,
	DEPARTMENT : "General"
};
someStruct.GLAN = {
	AGE : 29,
	DEPARTMENT : "computer"
};
someStruct.GEORGE = {
	AGE : 31,
	DEPARTMENT : "Physical"
};
result = structSort( someStruct, "textnocase", "asc", "department" );
writeOutput( lcase( JSONSerialize( result ) ) );

```

Result: ["glan","scott","george"]

### Date sorting using callback

Compare values via dateCompare


```java
birthdays = { 
	"Jim" : "1982/12/5",
	"Anne" : "1968/9/13",
	"Thomas" : "1975/3/28"
};
sorted = structSort( birthdays, ( Any e1, Any e2 ) => {
	return dateCompare( e1, e2 );
} );
for( birthday in sorted ) {
	writeOutput( birthday & " (" & dateDiff( "yyyy", birthdays[ birthday ], now() ) & "), " );
}

```

Result: Anne (49), Thomas (42), Jim (35),

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjc0KgkAURtczT%2FExKwVBKtoULsQggsBAofVoAwqjxjhThPjuTamE0ur%2BnO%2Bey%2Buy4rJFgA6URPEVO3SUkDROw7PtV2tKeo%2BSy%2Bm4INsBRGG6ABsLaL%2Bnvo%2BkaJ7IjVKi1uDDK3ow1d2B5JmQAYvmkHl4cBWME9yfZVq1jdLihuwF3Wgu57bwf2i0JlqZXCeWOZPOA6tNJVSZ2wzj7bcMN3A%2F798Xg1W0" target="_blank">Run Example</a>

```java
animals = { 
	COW : {
		TOTAL : 12
	},
	PIG : {
		TOTAL : 5
	},
	CAT : {
		TOTAL : 3
	}
};
// Show current animals
Dump( label="Current animals", var=animals );
// Show animals sorted by total
Dump( label="Animals sorted by total", var=StructSort( animals, "numeric", "asc", "total" ) );

```


<a href="https://try.boxlang.io/?code=eJxdj81uwjAQhM%2FZpxjlFKQI1B6LcgiEG0KVOPVowgZZxDba2CCEePfaUArtyf52ZvbHnNdeQutR4QLKanwgXzq7dTYvKZsl%2FFSih0TzRDOWXt%2FEJuGKT%2Fhysk%2BFRSo0YZN0uk6JJhNsudOW0aq%2BVxvV7tEF23rtLD0%2BNy1JBWp7Br%2BV9%2FcdI1woE%2FZBosmZgxIuoGQXDFs%2FjBfR%2BULRP6UrnUR7boI5FDgqqczPfSXifO6ruH%2FnhDE48dru8hT6F7kH1tFQ4Bl%2FbInRb6u68yx%2FOn0Db5dpWQ%3D%3D" target="_blank">Run Example</a>

```java
myStruct = { 
	A : "London",
	B : "Paris",
	C : "Berlin",
	D : "New York",
	E : "Dublin"
};

// define callaback function
function callback( Any e1, Any e2 ) {
	return compare( arguments.E1, arguments.E2 );
}
writeDump( var=myStruct, label="Before sorting" );
writeDump( var=StructSort( myStruct, callback ), label="After sorting" );

```


