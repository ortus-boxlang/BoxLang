### Convert binary back into a string

`charsetEncode()` decodes binary data (a byte array) into a string using the given character set.

```java
bytes = binaryDecode( "4869", "hex" )
writeOutput( charsetEncode( bytes, "utf-8" ) )

```

Result: Hi

### Decode with a specific character set

```java
bytes = binaryDecode( "e9", "hex" )
writeOutput( charsetEncode( bytes, "iso-8859-1" ) )

```

Result: é

### Encode a string using utf-8 back into binary encoding of the string

```java
s = charsetDecode( "string", "utf-8" )
writeOutput( charsetEncode( s, "utf-8" ) )

```

Result: string

### Encode a string using us-ascii back into binary encoding of the string

```java
s = charsetDecode( "string", "us-ascii" )
writeOutput( charsetEncode( s, "us-ascii" ) )

```

Result: string
