### Fix 1.8 to an integer



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMus0FAw1LNQ0FTQBAB3wQe9" target="_blank">Run Example</a>

```java
writeOutput( fix( 1.8 ) )

```

Result: 1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMus0FAw1zM1MVPQVNDkUtDXVzAHALwWCQk%3D" target="_blank">Run Example</a>

```java
writeOutput( fix( 7.546 ) )
 // 7

```

### Positive number is rounded down

```java
writeOutput( fix( 3.7 ) )

```

Result: 3

### Negative number is rounded toward zero

```java
writeOutput( fix( -3.7 ) )

```

Result: -3
