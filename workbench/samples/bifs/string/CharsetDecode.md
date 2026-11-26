### Convert a string to binary

`charsetDecode()` converts a string into its binary (byte array) representation using the given character set.

```java
bytes = charsetDecode( "Hi", "utf-8" )
writeOutput( binaryEncode( bytes, "hex" ) )

```

Result: 4869

### Different character sets give different bytes

```java
utf8 = binaryEncode( charsetDecode( "é", "utf-8" ), "hex" )
latin1 = binaryEncode( charsetDecode( "é", "iso-8859-1" ), "hex" )
writeOutput( utf8 & " " & latin1 )

```

Result: c3a9 e9

### Member function

The encoding defaults to `utf-8`.

```java
writeOutput( binaryEncode( "Hi".charsetDecode(), "hex" ) )

```

Result: 4869

### Round trip with charsetEncode

```java
s = charsetDecode( "string", "utf-8" )
writeOutput( charsetEncode( s, "utf-8" ) )

```

Result: string
