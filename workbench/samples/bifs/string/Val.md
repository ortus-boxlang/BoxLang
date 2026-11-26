### Numeric characters at beginning and middle of a string



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKEvM0VBQMjQyKEktLjE0Ki4uKcrMS1dS0FTQBAAjZA0m" target="_blank">Run Example</a>

```java
writeOutput( val( "120test12sstring" ) )

```

Result: 120

### Numeric characters only at the end of a string



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKEvM0VBQKi4pysxLt7RQUtBU0AQAwuAKbg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( val( "string98" ) )

```

Result: 0

### Numbers, decimals and signs

```java
writeOutput( val( "1234 Main St." ) & "," & val( "Main St., 1234" ) & "," & val( "123.456" ) & "," & val( "-5x" ) )

```

Result: 1234,0,123.456,-5

### Empty strings and text return zero

```java
writeOutput( val( "" ) & "," & val( "one" ) & "," & val( "123T456" ) )

```

Result: 0,0,123

### Member function

```java
writeOutput( "42abc".val() )

```

Result: 42
