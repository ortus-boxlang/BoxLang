### Simple Example

Inserts 'foo' into the list at position 2

<a href="https://try.boxlang.io/?code=eJzLySwu8cwrTi0qcSzRUFBKSizSyckvSs3VySwoLs1V0lEw0lFQSsvPV1LQtOYCAGxYDi0%3D" target="_blank">Run Example</a>

```java
listInsertAt( "bar,lorem,ipsum", 2, "foo" );

```

Result: bar,foo,lorem,ipsum

### Example with Custom Delimiter

Inserts 'foo' into the list with a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwu8cwrTi0qcSzRUFBKSiyqyckvSs3VySwoLs2tyU2tKc7PTS3JyMxLV9JRMNJRUErLzweylGqUFDStuQDughUh" target="_blank">Run Example</a>

```java
listInsertAt( "bar|lorem,ipsum|me|something", 2, "foo", "|" );

```

Result: bar|foo|lorem,ipsum|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVzcEKwjAQhOF7nmLYUwvBIh6LEEEEQfEZTLpgoE1LsiGv7wq9eBwG%2Fq%2FlKPyqslXpMMci91Q4y0UXva31NliyOFrQmpjQox%2FNMODJi%2BeMW01B4prQonzgMPEcFw1mUyQ%2FtIazZpx3E42m%2FahrXbYO%2B3v4B0%2BqBNXI7dAXidwwnQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listInsertAt( "a,,b,c,", 1, "one" ) );
// Member Function with @ delimiter
strList = "a@b@d";
writeDump( strList.listInsertAt( 3, "c", "@" ) );

```


