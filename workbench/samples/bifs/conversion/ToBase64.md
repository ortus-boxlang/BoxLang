### Convert a string to Base64

```java
writeOutput( toBase64( "Test String" ) )
```

Result: VGVzdCBTdHJpbmc=

### Using the member function

```java
writeOutput( "ToBase64".toBase64() )
```

Result: VG9CYXNlNjQ=

### Convert a binary object to Base64

Text is converted to binary with `charsetDecode()` first.

```java
bytes = charsetDecode( "Hello", "utf-8" )
writeOutput( toBase64( bytes ) )
```

Result: SGVsbG8=

### Specify the character encoding

The encoding is used to convert the string to bytes before encoding.

```java
writeOutput( toBase64( "Hello", "utf-16" ) )
```

Result: /v8ASABlAGwAbABv
