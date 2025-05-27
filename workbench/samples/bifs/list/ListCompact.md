### Trims first and last comma from list string



<a href="https://try.boxlang.io/?code=eJzLySwucc7PLUhMLtFQUNJJTU8v1snNzMnWSSpKTUzRScvJLy3SUVLQtOYCAEI9DfM%3D" target="_blank">Run Example</a>

```java
listCompact( ",eggs,milk,bread,flour," );

```

Result: eggs,milk,bread,flour

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjssKwjAQRff9iiGrlhkMPnbFlS4VBb8g1tEG8iKZ2N%2B3UBG8uwuXc4%2FWcLM%2BOYZnDYPYGJopW%2BFLlVSlBWeLHKJPZpiLIqKrM0HolZkDFfNmYpNlJFLQQdc3WsOZ%2FZ3zjweTlRGGWiR6eLCzfsbnpkg%2BFYE9KERc4wa3uMMlql8cjtWnFpbh6l8Ev3cf5Ps80Q%3D%3D" target="_blank">Run Example</a>

```java
// Simple function
writeOutput( listCompact( ",,,Plant,green,save,earth,," ) );
// Member function with custom delimiter
strLst = "+++1+2+3+4+++++++";
writeDump( strLst.listCompact( "+" ) );

```


