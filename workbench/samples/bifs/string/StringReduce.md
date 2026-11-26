### Full function

Reduce the string to a single value.

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUEpMSk5JTVPiSs7JLy4tSgUKaSg45lUqZOYVGOrAWEYKmgq6dgrVXJxFqSWlRXlgWQU1sBRXLVd5UWZJqn9pSUFpiYZCcElRZl56UGpKaXKqhkIOxCYdBaj5OgpKVUpA0zQBxvMp4A%3D%3D" target="_blank">Run Example</a>

```java
letters = "abcdef"
closure = ( Any inp1, Any inp2 ) -> {
	return inp1 & inp2
}
writeOutput( StringReduce( letters, closure, "z" ) )

```

Result: zabcdef

### Sum the digits of a string

```java
writeOutput( stringReduce( "12345", ( acc, ch ) -> acc + ch, 0 ) )

```

Result: 15

### Count the vowels

```java
writeOutput( stringReduce( "boxlang", ( acc, ch ) => acc + ( "aeiou".find( ch ) > 0 ? 1 : 0 ), 0 ) )

```

Result: 2
