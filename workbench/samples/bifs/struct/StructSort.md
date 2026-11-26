### Numeric sorting



<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4gxydVGwUrA01uHijHT18fEPB%2FEMgDz3IFdXPxDHhKvWmqsotbg0B6SnGKw5OL%2BoREOhGG6YjoJSXmlualFmshKQmZJanKykoGnNVV6UWZLqX1pSUApUnZOcWJyqoeAV7O8XDFSZmJNZBeRCDdYEQWsuAHqvL%2B4%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	RED : 93,
	YELLOW : 90,
	GREEN : 94
}
result = structSort( someStruct, "numeric", "desc" )
writeOutput( lcase( JSONSerialize( result ) ) )

```

Result: ["green","red","yellow"]

### Sort by subelement



<a href="https://try.boxlang.io/?code=eJxljs0KwjAQhM%2FNUyw5VSiCCoIWD0VLQbQV0xcIccFCfyTZ4h%2B%2Bu4kUtZQ9LDvsNzOmqVCQbhXBCp6vkJmvMBbrLM%2BdzLwoiWEJ03nAvE18iI75Pk5zq%2FAEa9Sy5KyPJrso7ZGLAama6tIS6gEaZ0fL%2FMGzyQA%2BnO%2BmUF2uRtOWrr%2F5OIhGkw8%2FxwA44Y3qRkmD3F7SKLdOeJGaKqyJwyhkV10QZi3ZUj6U7teHrchSgbqQZfGwZxc0chOyN5rWX20%3D" target="_blank">Run Example</a>

```java
someStruct = {}
someStruct.SCOTT = {
	AGE : 26,
	DEPARTMENT : "General"
}
someStruct.GLAN = {
	AGE : 29,
	DEPARTMENT : "computer"
}
someStruct.GEORGE = {
	AGE : 31,
	DEPARTMENT : "Physical"
}
result = structSort( someStruct, "textnocase", "asc", "department" )
writeOutput( lcase( JSONSerialize( result ) ) )

```

Result: ["glan","scott","george"]

### Sort with a callback

The callback receives two key names, so a closure is used to look up the values in the outer struct.

```java
birthdays = { Jim : "1982/12/5", Anne : "1968/9/13", Thomas : "1975/3/28" }
sorted = structSort( birthdays, ( k1, k2 ) => dateCompare( birthdays[ k1 ], birthdays[ k2 ] ) )
writeOutput( sorted.toList( " " ) )
```

Result: Anne Thomas Jim

### Sort by a nested value

With a path, the sort uses the named value inside each nested struct.

```java
animals = { cow : { total : 12 }, pig : { total : 5 }, cat : { total : 3 } }
writeOutput( structSort( animals, "numeric", "asc", "total" ).toList() )
```

Result: cat,pig,cow

### Invalid sort directive

```java
try {
    structSort( { a : 1 }, "bogus", "asc" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: The sort directive [bogus,asc] is not a valid struct sorting directive
