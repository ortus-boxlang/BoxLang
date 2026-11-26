### Compare Two Dates by Year



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSEksSXXOzy1ILErVUFAyNNI3NtA3MjA0VdIB8wyM4LxKIFBS0FTQBACHQhFj" target="_blank">Run Example</a>

```java
writeOutput( dateCompare( "12/30/2015", "12/02/2015", "yyyy" ) )

```

Result: 0

### Compare Two Dates by Day

Returns 1 because date1 is greater than date 2

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSEksSXXOzy1ILErVUFAyNNI3NtA3MjA0VdIB8wyM4LwUJQVNBU0ATwsP4w%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( dateCompare( "12/30/2015", "12/02/2015", "d" ) )

```

Result: 1

### Member function example



<a href="https://try.boxlang.io/?code=eJxLMVSwVUguSk0sSXUBYg0FJSMDIxMlHQUlA0MoqaDJlWKEU5URiDQ0AakqL8osSfUvLSkoLdFQSDHUS87PLUgsAqoGatdU0AQAl9obPw%3D%3D" target="_blank">Run Example</a>

```java
d1 = createDate( "2024", "01", "01" )
d2 = createDate( "2024", "02", "14" )
writeOutput( d1.compare( d2 ) )

```

Result: -1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSEksSXXOzy1ILErVUMjLL9fQ1FFQMjTUNzTQN7S0NFJS0FRQU1BS0HABqjNUyCxWyAEyihRKMhLzwHqNNG2SiuyAyrjKcRqLbB4B01MLSxNzFEryyTQb7AGooQgzE4tyMlHcDDQSACMqS24%3D" target="_blank">Run Example</a>

```java
writeOutput( dateCompare( now(), "11/10/1992" ) & " (Date1 is later than date2)<br>" )
writeOutput( dateCompare( "11/10/1992", "11/10/1992" ) & " (Date1 is equal to date2)<br>" )
writeOutput( dateCompare( "11/10/1992", now() ) & " Date1 is earlier than date2" )

```


<a href="https://try.boxlang.io/?code=eJx1zsEKgkAQBuC7T%2FHjIXYh0vEmVJe69wxTu5Cgrm0j4ts3UZkSHQZmYP5vxmGHS%2FQs%2FqhlMHqOBm0YjIVdowmtXGez43GaYBNHGk%2BJMsozKssiTYZYiT%2F10vVi4DaH0HQc1Z0vaXSFFM%2BDhOqOWpsIuXKrvvhie457XUryv5p7E2Yy%2FK3nGhJegv0QS4B%2BhS%2FAsa4Wb2j%2BAc%2BiU2M%3D" target="_blank">Run Example</a>

```java
d = createDate( year( now() ), month( now() ), day( now() ) )
d1 = "11/10/1992"
writeOutput( d.Compare( "11/10/1992" ) & " Date1 is later than date2<br>" )
0
writeOutput( d.Compare( d ) & " (Date1 is equal to date2)<br>" )
writeOutput( d1.Compare( d ) & " Date1 is earlier than date2" )

```


### Compare down to the hour

Date1 is earlier than date2, so the result is -1.

```java
writeOutput( dateCompare( createDateTime( 2024, 1, 1, 9, 0, 0 ), createDateTime( 2024, 1, 1, 17, 0, 0 ), "h" ) )
```

Result: -1

### The same day compares equal at day precision

```java
writeOutput( dateCompare( createDateTime( 2024, 1, 1, 9, 0, 0 ), createDateTime( 2024, 1, 1, 17, 0, 0 ), "d" ) )
```

Result: 0

### An unsupported datepart throws an error

```java
try {
	dateCompare( createDate( 2024, 1, 1 ), createDate( 2024, 1, 2 ), "ww" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Invalid datepart: ww
