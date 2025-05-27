### Tag Example

 


```java
<bx:set length = 10 >
<bx:set result = RJustify( "abc", length ) >
<bx:output>
    <pre>~#result#~</pre>
</bx:output>
```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVVS0lEoSyyyDfIqLS7JTKvUUACJmChoKmhac5UToToxKRkoaEiqBlMkDfmlJQWlJUApm4wiOyWQcLGCLdAd1lxFqSBWEVxzMdhpqPak5mkogNSBzFPQ1wcJpJdkKGQWK5igG19QlGpXowxUrFxjow%2FioFoIchlWOw0NiLfU0IBEWw0NsFtqQbydFgStBFsHAFElmM4%3D" target="_blank">Run Example</a>

```java
writeDump( label="", var=RJustify( "", 4 ) );
writeDump( label="", var=RJustify( "abc", 1 ) );
writeDump( label="", var=RJustify( "abc", 5 ) );
writeoutput( "<hr>" );
s = "";
res = rJustify( s, 4 );
writeDump( len( res ) ); // length is 4
writeoutput( "<pre>|#res#|</pre><hr>" );
s = "abc";
res = rJustify( s, 10 );
writeDump( len( res ) ); // length is 10
writeoutput( "<pre>|#res#|</pre><hr>" );
s = "10";
res = rJustify( s, 8 );
writeDump( len( res ) ); // length is 8
writeoutput( "<pre>|#res#|</pre>" );

```


