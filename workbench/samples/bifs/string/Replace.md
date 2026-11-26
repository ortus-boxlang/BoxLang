### Replace only the first occurrence

By default the scope is `one`, so only the first occurrence is replaced.

```java
writeOutput( replace( "Hello World", "o", "0" ) )

```

Result: Hell0 World

### Replace every occurrence

```java
writeOutput( replace( "Hello World", "o", "0", "all" ) )

```

Result: Hell0 W0rld

### The search is case-sensitive

```java
writeOutput( replace( "Boxlang", "U", "u" ) )

```

Result: Boxlang

### Example using Callback Function

You can pass in a callback function to the third argument of the `replace` function

<a href="https://try.boxlang.io/?code=eJwtjcEKwjAMhs%2F2KX562mD4AuJheB34DLFUKdSsZAkyxHe3pbskfEm%2BP67YI6eAp3HQtDKs3GiLA2beUUg1Ck8d1i21i06rpBdGfN1JoppUr2uHgvHifk7iZllxhcSSKdS1n%2FEmBqFya4GY8tlP8MS19ueV5mXxLeMjSePdtJgOOOLq%2BA%2FPgDrf" target="_blank">Run Example</a>

```java

public function upCase( Any pattern, Any position, Any orig ) {
	return uCase( pattern )
}
result = replace( "A man a plan a canal.", "an", upCase, "ALL" )
writeOutput( result )

```

Result: A mAN a plAN a cANal.

### Member function

```java
writeOutput( "a-b-c".replace( "-", "+", "all" ) )

```

Result: a+b+c

### Error: invalid scope

```java
try {
	replace( "abc", "a", "b", "some" )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Invalid replacement scope: [some]. Valid options are 'one' or 'all'.
