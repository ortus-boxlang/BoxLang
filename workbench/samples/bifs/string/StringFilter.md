### Full function

Return only the letters in the string that meet the callback condition.

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUKqqCgyMUuJKTszJSUpMzgYKaSg45lUqZOYVKGgq6NopVHNxFqWWlBblgYVsQVqUuGq58vNyKqNAJgSXFGXmpbtl5gBN1FDIgZisowA3UJOrvCizJNW%2FtKSgtERDAapPEwCvCyqu" target="_blank">Run Example</a>

```java
letters = "zzQQZ"
callback = ( Any inp ) -> {
	return inp == "z"
}
onlyZs = StringFilter( letters, callback )
writeOutput( onlyZs )

```

Result: zzZ

### Keep only the digits

```java
writeOutput( stringFilter( "a1b2c3", ( ch ) => isNumeric( ch ) ) )

```

Result: 123
