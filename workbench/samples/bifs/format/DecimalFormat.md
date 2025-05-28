### Formatting examples



<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSwpSU3xzCtRsFVISU3OzE3McQOLaigYGhkraFor6OuDWHoGBlzlRZklqf6lJQWlQNk0ZL1qCko2SUV2SkD1XHCJ8MySjODUgsSixJL8omIs5huAAcwOHSAbhHHbhGYgVktdIHZg2mZpYa5naAq1DMLBYQ3MCKzmB%2BWX5qWkpoQWYNpgYmqmZ25hCbUCzLNU0FUoAukoVigtwGEdwkRkCwEqj4HW" target="_blank">Run Example</a>

```java
formattedInt = decimalFormat( 123 ); // 123.00
writeOutput( formattedInt & "<br>" );
formattedWithSeparators = decimalFormat( 1000000 ); // 1,000,000.00
writeOutput( formattedWithSeparators & "<br>" );
formattedDecimal = decimalFormat( 987.15 ); // 987.15
writeOutput( formattedDecimal & "<br>" );
formattedRoundedUp = decimalFormat( 456.789 ); // 456.79 - rounds up
writeOutput( formattedRoundedUp & "<br>" );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSElNzsxNzHHLL8pNBHKVTEyVFDQV1BSUbJKK7IBMawV9fQUTUz0DA65yPPqMzIC6NK25QKqNzICqrbkAdSIdMg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( decimalFormat( "45" ) & "<br>" ); // 45.00
writeOutput( decimalFormat( 26 ) );
 // 26.00;

```


