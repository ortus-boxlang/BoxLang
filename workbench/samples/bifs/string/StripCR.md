### Removing carriage returns (cr) from a string



<a href="https://try.boxlang.io/?code=eJyrULBVUHJUUlBTSM5ILNJQMDRW0ARylJxQhay5KoEKi0uKMguSgUIVIJHyosyS1PzSkoLSEg0FJf%2BizPTMvMQcKwWQzgqQGQo%2BqXnpJRlWCso5qXlgTco2SUV2Spiag0EGF6SmQDRXYtFcCdQM1ggAdOswPQ%3D%3D" target="_blank">Run Example</a>

```java
x = "A" & char( 13 ) & "B" & char( 13 );
y = stripcr( x );
writeoutput( "Original: " & x & " Length: #len( x )#<br>" );
writeoutput( "Stripped: " & y & " Length: #len( y )#" );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLilSsFVQ8lTIyS9LVcgpTU5NVVJQU0jOSCzSUDA0VtC05ipKLQYqKS4pyixwBgoCGSDR8qLMklSX0twCDQWQAlSRnNQ8qEJsEmD1IAkAXJ4l1Q%3D%3D" target="_blank">Run Example</a>

```java
str = "I love lucee" & char( 13 );
res = stripCr( str );
writeDump( res );
writeDump( len( str ) );
writeDump( len( res ) );

```


