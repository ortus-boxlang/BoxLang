### Cast a string to a number

```java
writeOutput( toNumeric( "29.5" ) )
```

Result: 29.5

### Cast a hex value to a number

```java
writeOutput( toNumeric( "FF0011", "hex" ) )
```

Result: 16711697

### Cast a binary value to a number

```java
writeOutput( toNumeric( "1010", "bin" ) )
```

Result: 10

### Cast an octal value to a number

```java
writeOutput( toNumeric( "24", "oct" ) )
```

Result: 20

### Use a numeric radix

The radix can be any number from 2 to 36.

```java
writeOutput( toNumeric( "zz", 36 ) )
```

Result: 1295

### Invalid radix

A radix that is not 2-36, bin, oct, dec or hex throws an exception.

```java
try {
	toNumeric( "1", 99 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Radix must be between 2 and 36.

### Value that is not a number

```java
try {
	toNumeric( "abc" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Can't cast [abc] to a Number.
