### Tag Syntax

<a href="https://try.boxlang.io?code=eJxLSEjgskmqsCpOLVFIzs8tSCzKLM7PU7CFclI1FJSc8ityEvPSlXQUlJKgTAVNBTuwtpTS3AKFssQiWyVlhG5lJX07BRDgSkhIAAAqGR6x"  target="_blank">Run Example</a>


```java
<bx:set comparison = compare( "Boxlang", "boxlang" ) >
<bx:dump var="#comparison#"/>     
```

Result: -1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwzs8tSCxK1VBQMjQwUPBNzMxTCC7RU9JRUDIzN1VwTSwuUXAsS80rTVVS0FTQtFbQ11fQNeRKQdNriqbXUMG5tCC1qCQzL18hPLESoZewVhQ%2BWB8XSKMBFwC5nC4y" target="_blank">Run Example</a>

```java
dump( Compare( "100 Main St.", "675 East Avenue" ) ); // -1
dump( Compare( "500 Main St.", "1 Cupertino Way" ) ); // 1
dump( Compare( "500 Main St.", "500 Main St." ) );
 // 0

```


