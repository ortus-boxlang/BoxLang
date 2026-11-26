### Repeat a string

```java
writeOutput( repeatString( "Boxlang ", 3 ) )

```

Result: Boxlang Boxlang Boxlang 

### Build a separator line

```java
writeOutput( repeatString( "-", 5 ) )

```

Result: -----

### A count of zero returns an empty string

```java
writeOutput( "[" & repeatString( "ab", 0 ) & "]" )

```

Result: []

### Error: negative count

```java
try {
	repeatString( "ab", -1 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Count must be a non-negative number
