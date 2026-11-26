### Trim



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQULJTUlBTKCnKzAWyFYDAycclP7kYxFJS0ARKKdkAaQCHWAzE" target="_blank">Run Example</a>

```java
writeOutput( ">" & trim( "    BLDocs    " ) & "<" )

```

Result: >BLDocs<

### Trim specific characters

```java
writeOutput( trim( "--hello--", "-" ) )

```

Result: hello

### Member function

```java
writeOutput( "[" & "  hi  ".trim() & "]" )

```

Result: [hi]
