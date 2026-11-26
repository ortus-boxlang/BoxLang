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

<a href="https://try.boxlang.io/?code=eJwtjUEKAjEMRdf2FJ%2BuZmDwBi6K2wHPEEuVQk1LJkFEvLsdOpuEx8%2F7aXYvOeJhHDVXhrUrbWlC4A8aqSbhZUDd8n4xqEp%2BYsbXnSSpSfeGdiiY3c9J2qwoLpDUCsWe%2BoAXMQid9xWJqZz9Ak%2Fc5%2FjdKayr7xVvyZpups10wtE2%2FwHnsDoa" target="_blank">Run Example</a>

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
