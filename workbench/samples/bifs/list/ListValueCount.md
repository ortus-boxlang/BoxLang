### Simple Example

Counts instances of 'foo' in the list, ignoring case

<a href="https://try.boxlang.io/?code=eJzLySwuCUvMKU11zi%2FNK%2FHLd04sTtVQUErLz9dJSizSyckvSs3VcQPyMguKS3OVdMBSSgqa1lwAcZoThA%3D%3D" target="_blank">Run Example</a>

```java
listValueCountNoCase( "foo,bar,lorem,Foo,ipsum", "foo" );

```

Result: 2

### Example with Custom Delimiter

Counts instances of 'foo' in the list with a custom delimiter, ignoring case

<a href="https://try.boxlang.io/?code=eJzLySwuCUvMKU11zi%2FNK%2FHLd04sTtVQUEpKLKpxy8%2BvyckvSs3VySwoLs2tyU2tScvP1ynOz00tycjMSwfxatz8%2FZV0FJSATBBVo6Sgac0FAFkAHeg%3D" target="_blank">Run Example</a>

```java
listValueCountNoCase( "bar|Foo|lorem,ipsum|me|foo,something|foo|FOO", "foo", "|" );

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io?code=eJyVj0ELgkAQRu%2F%2BisFLCguCVwnWzEKQukR4XWWIBVtjd7b8%2BU2U5SWo07y5PL5Xa0ewhLAdxl6Zk8iLjXhxOdKEApCf1b6p8922bA5hFtysJlz78yWCmh1H1XssBm8ogp5%2F8VaGEEOcQZJARWCRvDULB%2BnvAt7xxQEtdso7BE3QKb6VcWicJn3F4CFNZ2mS0%2BRHOaHkMvlPWTpLY5TPbXc%2BE2xh" target="_blank">Run Example</a>

```java
List = "boxlang,ACF,boxlangExt,boxlang, ext,BOXLANGEXT";
writeDump( ListValueCount( list, "boxlang" ) ); // It return's 2
writeDump( ListValueCount( list, "boxlangExt" ) ); // It return's 2 because it case Insensitive
List2 = "boxlang@ACF@boxlangExt@boxlang@ext@BOXLANGEXT";
writeDump( ListValueCount( list2, "boxlang", "@" ) );

```


