### Example of Yes

Pass in a true value outputs Yes

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQqEwt9st3yy%2FKTQRySopKUxU0FTQB5pAMFg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( yesNoFormat( true ) )

```

Result: Yes

### Example of No

Pass in a false value outputs No

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQqEwt9st3yy%2FKTQRy0hJzilMVNBU0AfMcDGE%3D" target="_blank">Run Example</a>

```java
writeOutput( yesNoFormat( false ) )

```

Result: No

### Numbers and strings are cast to booleans

```java
writeOutput( yesNoFormat( 1 ) & " " & yesNoFormat( 0 ) & " " & yesNoFormat( "yes" ) & " " & yesNoFormat( "no" ) )

```

Result: Yes No Yes No

### Member function

```java
writeOutput( true.yesNoFormat() )

```

Result: Yes
