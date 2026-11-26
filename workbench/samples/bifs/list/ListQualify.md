### Simple example for listQualify function with delimiter

To insert a string or character before and after the list elements.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErKr8hJzEu3KkrMzMm3MjQxsYKJmJgpcZUXZZak%2BpeWFJSWaCjkALUElibmZKZVQjg6Cko1SkDCSklBU0ETAI07Gmc%3D" target="_blank">Run Example</a>

```java
list = "boxlang:railo:144:boxlang:46"
writeOutput( listQualify( list, "|", ":" ) )

```

Result: |boxlang|:|railo|:|144|:|boxlang|:|46|

### Example for listQualify function with elements

To insert a string or character before and after the alphabet list elements only.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErKr8hJzEu3KkrMzMm3MjQxsYKJmJgpcZUXZZak%2BpeWFJSWaCjkALUElibmZKZVQjg6Cko1SkDCCkQ4ezgGKSloKmgCAGmuHBU%3D" target="_blank">Run Example</a>

```java
list = "boxlang:railo:144:boxlang:46"
writeOutput( listQualify( list, "|", ":", "CHAR" ) )

```

Result: |boxlang|:|railo|:144:|boxlang|:46

### Example for listQualify function with includeEmptyFields

If includeEmptyFields is true, empty value add in list elements.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErKr8hJzEu3KkrMzMm3MjQxsbKCCVmZmClxlRdllqT6l5YUlJZoKOQANQWWJuZkplVCODoKSjVKQMIKRDh7OAYB6ZKi0lQFTQVNAFAhHpU%3D" target="_blank">Run Example</a>

```java
list = "boxlang:railo:144::boxlang::46"
writeOutput( listQualify( list, "|", ":", "CHAR", true ) )

```

Result: |boxlang|:|railo|:144:||:|boxlang|:||:46

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjs0KwjAQhO99iiUHSaG4L6CC4kWoiASvQmtTXUjSkmyogg9vgj%2FgYXdmYflmEEGRHY2GProL0%2BBgIr5BR32vvXYMnTZkibWXWBaTT%2B4QeYwswVDgY2wM9Q8JIrAnd8XNcDdN0hADYb1WogJxzgsFlDADsWj9Kk86C0TYa9tq%2F0svEqZOXFiC%2BKCqryZapU5qJ941ttGOEtJ%2F7jH%2FL%2FPMYeULPDhItQ%3D%3D" target="_blank">Run Example</a>

```java
// Simple function with different delimiter(/)
writeOutput( listQualify( "string/Boxlang/susi/LAS", "^", "/" ) & "<br><br>" )
// Member function
strList = "Boxlang,Boxlang,LAS,SUSI"
writeDump( strlist.listQualify( "|" ) )

```


