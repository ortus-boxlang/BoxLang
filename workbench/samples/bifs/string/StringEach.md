### Full function



<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUErMKCyvUuJKTszJSUpMzgYKaSg45lUqZOYVKGgq2NopVHNxlhdllqTml5YUlJZogCVsgRoLlRQ0uWq5gkuKMvPSXROTMzQUciDG6ijATdMEAAoNIbY%3D" target="_blank">Run Example</a>

```java
letters = "ahqwz"
callback = ( Any inp ) => {
	writeoutput( inp == "q" )
}
StringEach( letters, callback )

```

Result: falsefalsetruefalsefalse

### Use the index of each character

```java
stringEach( "abc", ( ch, i ) => writeOutput( i & ch & " " ) )

```

Result: 1a 2b 3c 
