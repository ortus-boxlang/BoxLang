### Decode a Base64 string to binary

```java
encoded = toBase64( "Hello, World!" )
result = toBinary( encoded )
writeOutput( isBinary( result ) )
```

Result: true

### Read the decoded bytes as text

```java
bytes = toBinary( "SGVsbG8=" )
writeOutput( charsetEncode( bytes, "utf-8" ) )
```

Result: Hello

### Using the member function

Missing padding characters are added for you.

```java
writeOutput( charsetEncode( "SGVsbG8".toBinary(), "utf-8" ) )
```

Result: Hello

### Invalid Base64 characters

```java
try {
	toBinary( "!!!" )
} catch ( any e ) {
	writeOutput( e.message.contains( "Illegal character" ) )
}
```

Result: true
