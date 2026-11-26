### Left Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFPICSnKzNVQUFIAAmc3l%2FzkYhBLSUETKKdko2TNBQCs%2FwgD" target="_blank">Run Example</a>

```java
">" & lTrim( "    BLDocs    " ) & "<"

```

Result: >BLDocs    <

### Trim specific characters

When `chars` is provided, each of its characters is trimmed from the start instead of whitespace.

```java
writeOutput( lTrim( "xxhello", "x" ) )

```

Result: hello

### Member function

```java
writeOutput( ">" & "  hi".lTrim() & "<" )

```

Result: >hi<
