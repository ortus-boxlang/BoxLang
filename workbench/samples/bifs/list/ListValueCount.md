### Simple Example

Counts instances of 'foo' in the list, ignoring case

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglLzClNdc4vzSvxy3dOLE7VUFBKy8%2FXSUos0snJL0rN1XED8jILiktzlXTAUkoKmgqaALq1GIw%3D" target="_blank">Run Example</a>

```java
writeOutput( listValueCountNoCase( "foo,bar,lorem,Foo,ipsum", "foo" ) )

```

Result: 2

### Example with Custom Delimiter

Counts instances of 'foo' in the list with a custom delimiter, ignoring case

<a href="https://try.boxlang.io/?code=eJwVxzEKwCAMQNGrBKcK3kJwrFt3C2krqBFN6JLDt07%2F%2FXdkxijchTcoefKRiqAnabyTTxM3MGcaGoi00MDqcp9StaJeRG5SRX5yu9dpiNE4MD9X1IAF%2BwEkkiLw" target="_blank">Run Example</a>

```java
writeOutput( listValueCountNoCase( "bar|Foo|lorem,ipsum|me|foo,something|foo|FOO", "foo", "|" ) )

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io?code=eJyVj0ELgkAQRu%2F%2BisFLCguCVwnWzEKQukR4XWWIBVtjd7b8%2BU2U5SWo07y5PL5Xa0ewhLAdxl6Zk8iLjXhxOdKEApCf1b6p8922bA5hFtysJlz78yWCmh1H1XssBm8ogp5%2F8VaGEEOcQZJARWCRvDULB%2BnvAt7xxQEtdso7BE3QKb6VcWicJn3F4CFNZ2mS0%2BRHOaHkMvlPWTpLY5TPbXc%2BE2xh" target="_blank">Run Example</a>

```java
List = "boxlang,ACF,boxlangExt,boxlang, ext,BOXLANGEXT"
writeDump( ListValueCount( list, "boxlang" ) ); // It return's 2
writeDump( ListValueCount( list, "boxlangExt" ) ); // It return's 2 because it case Insensitive
List2 = "boxlang@ACF@boxlangExt@boxlang@ext@BOXLANGEXT"
writeDump( ListValueCount( list2, "boxlang", "@" ) )

```

### Case-sensitive count

```java
writeOutput( listValueCount( "a,b,a,A", "a" ) )

```

Result: 2

### Case-insensitive member function

```java
writeOutput( "a,b,a,A".listValueCountNoCase( "a" ) )

```

Result: 3
