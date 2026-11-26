### Right Trim



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQULJTUlBTKAopyswFchSAwMnHJT%2B5GMRSUtAEyinZAGkAl%2FcNFg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( ">" & rTrim( "    BLDocs    " ) & "<" )

```

Result: >    BLDocs<

### Trim specific characters

When `chars` is provided, each of its characters is trimmed from the end instead of whitespace.

```java
writeOutput( rTrim( "helloxx", "x" ) )

```

Result: hello

### Member function

```java
writeOutput( "hi  ".rTrim() & "|" )

```

Result: hi|
