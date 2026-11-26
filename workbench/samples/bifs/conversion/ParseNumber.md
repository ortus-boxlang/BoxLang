### Parse a binary string

With the `bin` radix the string is read as a base 2 number.

```java
writeOutput( parseNumber( "1010", "bin" ) )
```

Result: 10

### Parse a hex string

```java
writeOutput( parseNumber( "FF", "hex" ) )
```

Result: 255

### Parse other numeral systems

`oct` is base 8 and `dec` is base 10.

```java
writeOutput( parseNumber( "17", "oct" ) & "," & parseNumber( "12.5", "dec" ) )
```

Result: 15,12.5

### Parse a number using a locale

Thousands and decimal separators depend on the locale.

```java
writeOutput( parseNumber( "1,234.56", "en_US" ) & "," & parseNumber( "1.234,56", "de_DE" ) )
```

Result: 1234.56,1234.56

### Using the radix argument by name

```java
writeOutput( parseNumber( number = "ff", radix = "hex" ) )
```

Result: 255

### Using the lsParseNumber alias

`lsParseNumber()` is an alias of `parseNumber()`.

```java
writeOutput( lsParseNumber( "1.234,5", "de_DE" ) )
```

Result: 1234.5

### A value that is not a number

A string that cannot be parsed throws an exception.

```java
try {
	parseNumber( "abc" )
} catch ( any e ) {
	writeOutput( "not a number" )
}
```

Result: not a number
