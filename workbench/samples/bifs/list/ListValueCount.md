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

<a href="https://try.boxlang.io/?code=eJyNjzELwjAQhff%2BikcXWwgUshYhUiMUOqq41nJDoEZpLurP9zTaTeh03w3v3X2dC4w18jEORGrT7NSH7JMTKJBgd2istad9XmePyTFt4%2BVWoJPssR8jNdfoucAou%2FpW5ShR1qgqtIyJOE5%2BFaCXxuX%2BnwacaehjIDjG0MtsfSAfHLs7Ze9KPesY0TG%2FugRGbMxyGz3rCJj0UfYCcgZjbA%3D%3D" target="_blank">Run Example</a>

```java
List = "lucee,ACF,luceeExt,lucee, ext,LUCEEEXT";
writeDump( ListValueCount( list, "lucee" ) ); // It return's 2
writeDump( ListValueCount( list, "luceeExt" ) ); // It return's 2 because it case Insensitive
List2 = "lucee@ACF@luceeExt@lucee@ext@LUCEEEXT";
writeDump( ListValueCount( list2, "lucee", "@" ) );

```


