### Formatting examples



<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSwpSU3xzCtRsFVISU3OzE3McQOLaigYGhkraFor6OuDWHoGBlzlRZklqf6lJQWlQNk0ZL1qCko2SUV2SgqaXHDx8MySjODUgsSixJL8omIsxhuAAcwKHSAbhHFbhGYgNjtdIFZgWmZpYa5naAq1C8LBYQvMCGzGB%2BWX5qWkpoQWYFpgYmqmZ25hCbUBzLNU0FUoAukoVigtwGEbwkSEfQDbVYDg" target="_blank">Run Example</a>

```java
formattedInt = decimalFormat( 123 ); // 123.00
writeOutput( formattedInt & "<br>" )
formattedWithSeparators = decimalFormat( 1000000 ); // 1,000,000.00
writeOutput( formattedWithSeparators & "<br>" )
formattedDecimal = decimalFormat( 987.15 ); // 987.15
writeOutput( formattedDecimal & "<br>" )
formattedRoundedUp = decimalFormat( 456.789 ); // 456.79 - rounds up
writeOutput( formattedRoundedUp & "<br>" )

```

Result: 123.00, 1,000,000.00, 987.15 and 456.79, each followed by a line break


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSElNzsxNzHHLL8pNBHKVTEyVFDQV1BSUbJKK7IBMawV9fQUTUz0DA65yPPqMzIC6NLlAio3MgIoBHSscsg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( decimalFormat( "45" ) & "<br>" ); // 45.00
writeOutput( decimalFormat( 26 ) )
 // 26.00

```

### Control the maximum number of decimal places

```java
writeOutput( decimalFormat( 456.789, 3 ) & " " & decimalFormat( 456.789, 1 ) )
```

Result: 456.789 456.8

### Using the member function

```java
writeOutput( (1234.5).decimalFormat() & " " & decimalFormat( "" ) )
```

Result: 1,234.50 0.00
