### Right Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFMoCinKzNVQUFIAAmc3l%2FzkYhBLSUETKKdko2TNBQCtxQgJ" target="_blank">Run Example</a>

```java
">" & rTrim( "    BLDocs    " ) & "<"

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
