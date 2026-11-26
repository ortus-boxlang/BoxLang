### Left Trim



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQULJTUlBTyAkpyswFchSAwMnHJT%2B5GMRSUtAEyinZAGkAlzENEA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( ">" & lTrim( "    BLDocs    " ) & "<" )

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
