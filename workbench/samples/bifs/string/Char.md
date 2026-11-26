### Get the character for an ASCII code

```java
result = char( 65 )
writeOutput( result )

```

Result: A

### Multiple character codes

```java
result = char( 65 ) & char( 66 ) & char( 67 )
writeOutput( result )

```

Result: ABC

### Codes above 65535 return an empty string

```java
writeOutput( "[" & char( 70000 ) & "]" )

```

Result: []

### Error: negative code

```java
try {
	char( -1 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Code must be a non-negative number
