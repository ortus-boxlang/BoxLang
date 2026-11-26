### Using left() on a string

In this example we'll use left() to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyElNA5JKIRmpCoWlmcnZCklF%2BeV5Cmn5FQpZpbkFqSkK%2BWWpRQolQPmcxKpKhZT8dCUdBUNLBU0FTQDP%2Fxiz" target="_blank">Run Example</a>

```java
writeOutput( left( "The quick brown fox jumped over the lazy dog", 19 ) )

```

Result: The quick brown fox

### Using left() with a negative count on a string

In this example we'll use a negative count to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyElNA5JKIRmpCoWlmcnZCklF%2BeV5Cmn5FQpZpbkFqSkK%2BWWpRQolQPmcxKpKhZT8dCUdBV0jUwVNBU0A6OMY3Q%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( left( "The quick brown fox jumped over the lazy dog", -25 ) )

```

Result: The quick brown fox

### Count larger than the string

```java
writeOutput( left( "abcd", 10 ) )

```

Result: abcd

### Member function

```java
writeOutput( "abcd".left( 2 ) )

```

Result: ab

### Error: count of zero

```java
try {
	left( "abcd", 0 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Count cannot be zero
