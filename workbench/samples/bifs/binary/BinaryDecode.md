### Decode a hex string into binary

The result is a byte array. Here it is re-encoded as hex to show its contents.

```java
bytes = binaryDecode( "F62B", "hex" )
writeOutput( bytes.len() & "," & binaryEncode( bytes, "hex" ) )
```

Result: 2,f62b

### Decode a Base64 string

The decoded bytes are converted back to text with `charsetEncode()`.

```java
bytes = binaryDecode( "U3RyaW5n", "base64" )
writeOutput( charsetEncode( bytes, "utf-8" ) )
```

Result: String

### Decode a UNIX UUencoded string

```java
bytes = binaryDecode( "&4W1R:6YG", "UU" )
writeOutput( charsetEncode( bytes, "utf-8" ) )
```

Result: String

### Decode a Base64URL string

The URL safe alphabet uses `-` and `_` instead of `+` and `/`.

```java
bytes = binaryDecode( "YT9iPg", "base64url" )
writeOutput( charsetEncode( bytes, "utf-8" ) )
```

Result: a?b>

### Create a byte array with 16 bytes

Each byte in the array is set to 0.

```java
bytes = binaryDecode( "00000000000000000000000000000000", "hex" )
writeOutput( arrayLen( bytes ) )
```

Result: 16

### Invalid encoding

An unsupported encoding name throws an exception.

```java
try {
	binaryDecode( "abc", "rot13" )
} catch ( any e ) {
	writeOutput( e.message.left( 29 ) )
}
```

Result: The encoding argument [rot13]
