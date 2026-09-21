### Dump Server Scope



<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUChOLSpLLVLQtOYCAFhRBy8%3D" target="_blank">Run Example</a>

```java
writeDump( server );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwlzLEOgjAUheGdpzhhgsTIAxg2HdxZdLvCoTRpbxu4qLy9Tdz%2BfMPfdXikHaMopj1miB6wxavDwpVn3GQrkLCRxYkxqVENaS4Zc%2BAXk5jAjszqs3rjtWwavGXtHW3wkc%2BkvOucmvaEIC%2BGvh7E%2FbNGe6l%2B5%2B8tJw%3D%3D" target="_blank">Run Example</a>

```java
// You can dump any thing here. Easy to see the content of complex data type
writeDump( var=getTimeZoneInfo(), label="Tag label" );

```


### Limit recursion depth

Prevents dumping deeply nested structures by limiting how many levels are recursed into.
`depth` is 1-based: `-1` (the default) is unlimited, `0` shows nothing, `1` shows the top
level with no recursion, `2` recurses once, etc.

```java
writeDump( var=complexObject, depth=3 );

```

### Limit the number of rows/items shown

Limits how many keys, array elements, or query rows are shown per level, independently of
recursion depth. Same 1-based semantics as `depth`.

```java
writeDump( var=bigArray, maxRows=10 );

```

### Deprecated: top

`top` is deprecated in favor of `maxRows` and `depth` above. For backwards compatibility it is
still accepted and, when `maxRows` is not also passed, its value is used as `maxRows`. A
deprecation warning is logged when it's used.

```java
writeDump( var=bigArray, top=10 );

```


