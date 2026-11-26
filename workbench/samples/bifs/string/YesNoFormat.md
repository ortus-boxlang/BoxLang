### Example of Yes

Pass in a true value outputs Yes

<a href="https://try.boxlang.io/?code=eJyrTC32y3fLL8pNLNFQKCkqTVXQtOYCAFcYBw4%3D" target="_blank">Run Example</a>

```java
yesNoFormat( true )

```

Result: Yes

### Example of No

Pass in a false value outputs No

<a href="https://try.boxlang.io/?code=eJyrTC32y3fLL8pNLNFQSEvMKU5V0LTmAgBeoAdZ" target="_blank">Run Example</a>

```java
yesNoFormat( false )

```

Result: No

### Example of empty string

Pass in an empty string outputs No


```java
yesNoFormat( "" )

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
