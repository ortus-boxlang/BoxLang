### Checks if toBase64() function returns binary data 

toBase64() returns base64 encoded data which is not binary

<a href="https://try.boxlang.io/?code=eJzLLHbKzEssqtRQKMl3SixONTPRUDBU0FTQBABvrgdd" target="_blank">Run Example</a>

```java
isBinary( toBase64( 1 ) )

```

Result: false

### Checks if toBinary() function returns binary data 

toBinary() expects base64 encoded data and returns binary data

<a href="https://try.boxlang.io/?code=eJzLLHbKzEssqtRQKMlHYiUWp5qZaCgYKmiCIAD0Pws2" target="_blank">Run Example</a>

```java
isBinary( toBinary( toBase64( 1 ) ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1z80KwjAMAOC7TxF6WkFkA%2FG2g8OLl130BTIJUuiPpK1lb2%2B3gRfNKYHky09hk%2BiS3asBixPZXg0hWEIPb7SZ1L5G7k0cjEeeG0icCTToXfmBY3bE5iHAru1aAd4SG%2F8UnJrWRAn2zIyzQHGpjVTq8kX%2F9VuvMOAevm%2BHASOdjvWeK6ADhLgefVDbZNAfCFRrZA%3D%3D" target="_blank">Run Example</a>

```java
writeDump( label="Boolean value", var=isBinary( true ) )
writeDump( label="Numeric value", var=isBinary( 1010 ) )
writeDump( label="String value", var=isBinary( "binary" ) )
writeDump( label="Array value", var=isBinary( arrayNew( 1 ) ) )
writeDump( label="Binary value", var=isBinary( ToBinary( toBase64( "I am a string." ) ) ) )

```


