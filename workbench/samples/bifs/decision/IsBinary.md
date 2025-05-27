### Checks if toBase64() function returns binary data 

toBase64() returns base64 encoded data which is not binary

<a href="https://try.boxlang.io/?code=eJzLLHbKzEssqtRQKMl3SixONTPRUDBU0FTQtOYCAH7oB6I%3D" target="_blank">Run Example</a>

```java
isBinary( toBase64( 1 ) );

```

Result: false

### Checks if toBinary() function returns binary data 

toBinary() expects base64 encoded data and returns binary data

<a href="https://try.boxlang.io/?code=eJzLLHbKzEssqtRQKMlHYiUWp5qZaCgYKmiCoDUXAAs6C3s%3D" target="_blank">Run Example</a>

```java
isBinary( toBinary( toBase64( 1 ) ) );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1j88KwjAMh%2B8%2BRehpA5ENxIvsYNnFyy76ApkEKfSPpK1lb2%2B7gRftKYHk%2B%2FJLYhVojObVgMaZ9CCkc5rQwht1JLHPlQflpbLISwOBI0EL7XmXfsgpGmL1qJB913c18hZY2WcFFPPaiBp8YcalwmKZTZTy%2BYL%2FF2zLFcPdfV93Ej2djjnRFdAAgl9jH8SmLvIPTkxslQ%3D%3D" target="_blank">Run Example</a>

```java
writeDump( label="Boolean value", var=isBinary( true ) );
writeDump( label="Numeric value", var=isBinary( 1010 ) );
writeDump( label="String value", var=isBinary( "binary" ) );
writeDump( label="Array value", var=isBinary( arrayNew( 1 ) ) );
writeDump( label="Binary value", var=isBinary( ToBinary( toBase64( "I am a string." ) ) ) );

```


