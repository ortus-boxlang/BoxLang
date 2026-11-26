### Create an Attempt with a value

Wraps a value in an Attempt object for fluent error-safe operations.

```java
attempt = attempt( 42 )
writeOutput( attempt.get() )

```

Result: 42

### Transform the value with map

`map()` applies a function to the value and returns a new Attempt.

```java
result = attempt( "hello" ).map( ( x ) -> x.len() * 2 ).getOrDefault( 0 )
writeOutput( result )

```

Result: 10

### Fall back when the Attempt is empty

`orElse()` returns the wrapped value, or the fallback when the Attempt is empty.

```java
writeOutput( attempt().orElse( "fallback" ) )

```

Result: fallback

### Attempt with no value

Creates an empty Attempt object for later use.

```java
attempt = attempt()
writeOutput( attempt.isEmpty() )

```

Result: true
