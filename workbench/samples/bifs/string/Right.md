### Using right() on a string

In this example we'll use right() to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKMpMzwBSSiEZqQqFpZnJ2QpJRfnleQpp%2BRUKWaW5BakpCvllqUUKJUD5nMSqSoWU%2FHQlHQULBU0FTQDXDxj0" target="_blank">Run Example</a>

```java
writeOutput( right( "The quick brown fox jumped over the lazy dog", 8 ) )

```

Result: lazy dog

### Using right() with a negative count on a string

In this example we'll use a negative count right() to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKMpMzwBSSiEZqQqFpZnJ2QpJRfnleQpp%2BRUKWaW5BakpCvllqUUKJUD5nMSqSoWU%2FHQlHQVdYyMFTQVNAAnBGU4%3D" target="_blank">Run Example</a>

```java
writeOutput( right( "The quick brown fox jumped over the lazy dog", -32 ) )

```

Result: the lazy dog

### Count larger than the string

```java
writeOutput( right( "abcd", 10 ) )

```

Result: abcd

### Member function

```java
writeOutput( "abcdef".right( 3 ) )

```

Result: def

### Error: count of zero

```java
try {
	right( "abcd", 0 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Count cannot be zero.
