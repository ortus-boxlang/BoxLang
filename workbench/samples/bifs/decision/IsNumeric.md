### Simple Lsisnumeric Example

Check whether the string is number or not in locale

<a href="https://try.boxlang.io/?code=eJzLKfYs9ivNTS3KTNZQUApJzVNS0AQATCsGaw%3D%3D" target="_blank">Run Example</a>

```java
lsIsNumeric( "Ten" )

```

Result: false

### Simple Lsisnumeric Example

Check whether the string is number or not in locale

<a href="https://try.boxlang.io/?code=eJzLKfYs9ivNTS3KTNZQUDIxM7dQUtAEAE%2F%2BBh0%3D" target="_blank">Run Example</a>

```java
lsIsNumeric( "4678" )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSc0vLSkoLdFQyCn2LPYrzU0tykzWUFAyNDJWUtBUUFNQskkqsgMyucpxK07LLEslXrWxnpEh8aoNDAyNSFGtZwA2HK8qQz1jI0MdAwNUc60V9PUV0hJzilOJ0qujoJSeWpSbmIfmOpApJUWlqQCx%2B2br" target="_blank">Run Example</a>

```java
writeoutput( lsIsNumeric( "123" ) & "<br>" )
writeoutput( lsIsNumeric( "five" ) & "<br>" )
writeoutput( lsIsNumeric( "3.21" ) & "<br>" )
writeoutput( lsIsNumeric( "0012" ) & "<br>" )
writeoutput( lsIsNumeric( "00.01" ) )
writeoutput( lsIsNumeric( "1.321,00" ) & "<br>" ); // false
writeoutput( lsIsNumeric( "1.321,00", "german" ) & "<br>" )
 // true

```


