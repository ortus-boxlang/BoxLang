### Fix 1.8 to an integer



<a href="https://try.boxlang.io/?code=eJxLy6zQUDDUs1DQtOYCABVtArU%3D" target="_blank">Run Example</a>

```java
fix( 1.8 )

```

Result: 1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSMus0FAw1zM1MVPQVNC05lLQ11cw5wIAzysJTg%3D%3D" target="_blank">Run Example</a>

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
