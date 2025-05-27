### Creates an ODBC time object

Uses the CreateODBCTime function to create an ODBC time object


```java
<bx:set time = createDateTime( 2012, 12, 12, 12, 12, 12 ) >
<bx:set result = createODBCTime( time ) >
<bx:output>#result#</bx:output>
```

Result: {t '12:12:12'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwLkpNLEn1d3FyDsnMTdVQyMsv19BUAEJrLgC6Vgm%2B" target="_blank">Run Example</a>

```java
dump( CreateODBCTime( now() ) );

```


