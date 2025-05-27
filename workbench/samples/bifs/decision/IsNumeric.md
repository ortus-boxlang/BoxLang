### Simple Lsisnumeric Example

Check whether the string is number or not in locale

<a href="https://try.boxlang.io/?code=eJzLKfYs9ivNTS3KTNZQUApJzVNS0LTmAgBZgQaw" target="_blank">Run Example</a>

```java
lsIsNumeric( "Ten" );

```

Result: false

### Simple Lsisnumeric Example

Check whether the string is number or not in locale

<a href="https://try.boxlang.io/?code=eJzLKfYs9ivNTS3KTNZQUDIxM7dQUtC05gIAXLgGYg%3D%3D" target="_blank">Run Example</a>

```java
lsIsNumeric( "4678" );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSc0vLSkoLdFQyCn2LPYrzU0tykzWUFAyNDJWUtBUUFNQskkqsgMyrbnKcatOyyxLJUG5sZ6RIQnKDQwMjUhSrmcANh6%2FMkM9YyNDHQMDNJMV9PUV0hJzilOJ0qujoJSeWpSbmIfuPpAxJUWlqVwAR9doVw%3D%3D" target="_blank">Run Example</a>

```java
writeoutput( lsIsNumeric( "123" ) & "<br>" );
writeoutput( lsIsNumeric( "five" ) & "<br>" );
writeoutput( lsIsNumeric( "3.21" ) & "<br>" );
writeoutput( lsIsNumeric( "0012" ) & "<br>" );
writeoutput( lsIsNumeric( "00.01" ) );
writeoutput( lsIsNumeric( "1.321,00" ) & "<br>" ); // false
writeoutput( lsIsNumeric( "1.321,00", "german" ) & "<br>" );
 // true

```


