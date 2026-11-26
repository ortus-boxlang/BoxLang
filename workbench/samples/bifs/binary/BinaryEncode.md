### Encode binary data as hex

The hex output is lowercase.

```java
bytes = binaryDecode( "737472696E67", "hex" )
writeOutput( binaryEncode( bytes, "hex" ) )
```

Result: 737472696e67

### Encode text as Base64

Text is first converted to binary with `charsetDecode()`.

```java
bytes = charsetDecode( "String", "utf-8" )
writeOutput( binaryEncode( bytes, "base64" ) )
```

Result: U3RyaW5n

### Encode using UNIX UUencode (UU)

```java
bytes = charsetDecode( "String", "utf-8" )
writeOutput( binaryEncode( bytes, "UU" ) )
```

Result: &4W1R:6YG

### Encode using Base64URL

The URL safe alphabet uses `-` and `_` instead of `+` and `/`.

```java
bytes = charsetDecode( "a?b>", "utf-8" )
writeOutput( binaryEncode( bytes, "base64url" ) )
```

Result: YT9iPg==

### The value must be binary

Passing a plain string instead of a byte array throws an exception.

```java
try {
	binaryEncode( "text", "hex" )
} catch ( any e ) {
	writeOutput( "not binary" )
}
```

Result: not binary
