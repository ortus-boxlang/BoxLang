### Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFMoKcrM1VBQUgACZzeX%2FORiEEtJQRMopWSjZM0FAKIqB7c%3D" target="_blank">Run Example</a>

```java
">" & trim( "    BLDocs    " ) & "<"

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
