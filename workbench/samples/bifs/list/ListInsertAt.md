### Simple Example

Inserts 'foo' into the list at position 2

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvHMK04tKnEE8pSSEot0cvKLUnN1MguKS3OVdBSMdBSU0vLzlRQ0FTQBdD8TNQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listInsertAt( "bar,lorem,ipsum", 2, "foo" ) )

```

Result: bar,foo,lorem,ipsum

### Example with Custom Delimiter

Inserts 'foo' into the list with a custom delimiter

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvHMK04tKnEE8pSSEotqcvKLUnN1MguKS3NrclNrivNzU0syMvPSlXQUjHQUlNLy84EspRolBU0FTQBQwBop" target="_blank">Run Example</a>

```java
writeOutput( listInsertAt( "bar|lorem,ipsum|me|something", 2, "foo", "|" ) )

```

Result: bar|foo|lorem,ipsum|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVzcEKwjAQhOF7nmLYUwuLRTwLEUQQFJ%2FBpAEDTVqSDXl9V%2FHicRj4v16ihEeTrcmAJVa55hqKnHTRk9mxZ2LsGbTmQBgxmmnCPSQXCi4te4lrRo%2FygsUclpi0V0yVctMYjlqxzs5k%2Bgc6t7QN%2BJ27f%2B6ghleL7Jd5A9TAL%2BI%3D" target="_blank">Run Example</a>

```java
writeOutput( listInsertAt( "a,,b,c,", 1, "one" ) )
// Member Function with @ delimiter
strList = "a@b@d"
writeDump( strList.listInsertAt( 3, "c", "@" ) )

```

### Insert at the end of the list

```java
writeOutput( listInsertAt( "a,b,c", 4, "d" ) )

```

Result: a,b,c,d
