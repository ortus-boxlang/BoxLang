### Simple Example

Deletes 2nd item from the list

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnFJzUktSXUE8pTS8vN1khKLdHLyi1JzdTILiktzlXQUjBQ0FTQBOmQSrw%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listDeleteAt( "foo,bar,lorem,ipsum", 2 ) )

```

Result: foo,lorem,ipsum

### Example with Custom Delimiter

Deletes 2nd item from the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnFJzUktSXUE8pTS8vN1khKLanLyi1JzdTILiktza3JTa4rzc1NLMjLz0pV0FIx0FJRqlBQ0FTQB%2B9YZow%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listDeleteAt( "foo,bar|lorem,ipsum|me|something", 2, "|" ) )

```

Result: foo,bar|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjkEKwjAQRfc5xdBVC4Nd6E5cCLUgKG68QFtHG0iTkExsj29SdBEQZjPw%2F%2FtvdpLpFtgGLkFJzw0pYjrGrzijMm%2FC3iyq0y9k8txKRQXCDiqo9lDXcFosDUwPMCsD8o6IiStNPTlogx5YGi08u0vcgQMU91F6jNchj7Ty8ZkGxJysmjDZEr7xTe62TQLin8CPmRE%2FZi1OQg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listDeleteAt( "I,love,boxlang,testFile", 4 ) ); // Expected output I,love,boxlang
// Member Function
strList = "This,is,a,the,test,file"
writeDump( strList.listDeleteAt( 3 ) )
 // Expected output This,is,the,test,file

```


