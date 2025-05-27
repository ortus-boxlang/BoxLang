### Example of Yes

Pass in a true value outputs Yes

<a href="https://try.boxlang.io/?code=eJyrTC32y3fLL8pNLNFQKCkqTVXQtOYCAFcYBw4%3D" target="_blank">Run Example</a>

```java
yesNoFormat( true );

```

Result: Yes

### Example of No

Pass in a false value outputs No

<a href="https://try.boxlang.io/?code=eJyrTC32y3fLL8pNLNFQSEvMKU5V0LTmAgBeoAdZ" target="_blank">Run Example</a>

```java
yesNoFormat( false );

```

Result: No

### Example of empty string

Pass in an empty string outputs No


```java
yesNoFormat( "" );

```

Result: No

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FCoTC3Oy3fLL8pNLNFQSEvMKU5V0FTQtFbQ11fwy%2BdKwVRTUlSKUBKZWoxNjQEBMwwJGaDkl69EwAwloFaIGi6YQQC0bj5j" target="_blank">Run Example</a>

```java
dump( yesnoFormat( false ) ); // No
dump( yesnoFormat( true ) ); // Yes
dump( yesnoFormat( 0 ) ); // No
dump( yesnoFormat( 1 ) ); // Yes
dump( yesnoFormat( "No" ) ); // No
dump( yesnoFormat( "Yes" ) );
 // Yes

```


