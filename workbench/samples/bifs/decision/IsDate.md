### Simple example

To determine whether a string can be converted to a date/time value.


```java
<bx:set Date = isNumericDate( now() ) >
<bx:output>#Date#</bx:output>
```

Result: Yes

### Simple example

To determine whether a string can be converted to a date/time value.


```java
<bx:set result = isNumericDate( "Monday" ) >
<bx:output>#result#</bx:output>
```

Result: No

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVUKzsxLz0lVyCvNTS3KTFYoS8wpTVXSAdJFtpnFfhBRl8SSVA0FQwVNBU2ucgwTPPNKUtNTi%2FBqNYABHGb45ZdraGLXmweSwqENpAKfvUoGhvogZKKEwwDfzLzSYrwm6BrCAS5DgkuKgIGI15TUtOKUNLB%2BAFUSe6U%3D" target="_blank">Run Example</a>

```java
writeDump( label="Single numeric value", var=isNumericDate( 1 ) )
writeDump( label="Integer value", var=isNumericDate( 1000000000 ) )
writeDump( label="Now()", var=isNumericDate( now() ) )
writeDump( label="Date value", var=isNumericDate( "01/01/04" ) )
writeDump( label="Minus value", var=isNumericDate( "-1111111111" ) )
writeDump( label="String value", var=isNumericDate( "efsdf" ) )

```


