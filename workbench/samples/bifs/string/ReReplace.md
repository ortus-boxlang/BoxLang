### Strip Characters Using ReReplace

This example strips out all characters except a-z and 0-9.

<a href="https://try.boxlang.io/?code=eJwrSg1KLchJTE71y3dOLE7VUFAqSS0uUTA0MlZU0lFQio5L1K0y0LWMBXFA2NHHR0lB05oLAOdQDrs%3D" target="_blank">Run Example</a>

```java
reReplaceNoCase( "test 123!", "[^a-z0-9]", "", "ALL" )

```

Result: test123

### Extract Characters Using Back Reference

Uses a back reference: \1 to extract the pattern contained within the parenthesis.

<a href="https://try.boxlang.io/?code=eJwrSg1KLchJTE71y3dOLE7VUFAyNDJOTEo2MTVT0lFQijbQtYzV1ohO1K2K1daE8EDiMYZKCprWXAA%2B8RAd" target="_blank">Run Example</a>

```java
reReplaceNoCase( "123abc456", "[0-9]+([a-z]+)[0-9]+", "\1" )

```

Result: abc

### Replace only the first match by default

```java
writeOutput( reReplace( "a1b2", "[0-9]", "_" ) )

```

Result: a_b2

### Replace every match with the all scope

```java
writeOutput( reReplace( "a1b2", "[0-9]", "_", "all" ) )

```

Result: a_b_

### Change the case of back references

`\u` uppercases the next character or group, `\l` lowercases it, and `\U`, `\L` and `\E` change the case of everything up to the next `\E`.

```java
writeOutput( reReplace( "hello world", "(\w+) (\w+)", "\u\1 \u\2" ) )

```

Result: Hello World

### Member function

```java
writeOutput( "a1b2".reReplace( "[0-9]", "_", "all" ) )

```

Result: a_b_
