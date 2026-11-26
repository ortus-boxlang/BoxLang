### Bitwise mask set

Sets 1 bit starting at bit 0 of `6` (`110` in binary) to the value 1.

```java
writeOutput( bitMaskSet( 6, 1, 0, 1 ) )
```

Result: 7

### Using a non zero start parameter

Sets the bit at position 2 of `10` (`1010` in binary) to 1.

```java
writeOutput( bitMaskSet( 10, 1, 2, 1 ) )
```

Result: 14

### Using non zero start and length parameters

Writes the 2 bit value `2` (`10` in binary) at bit 1 of `10` (`1010` in binary).

```java
writeOutput( bitMaskSet( 10, 2, 1, 2 ) )
```

Result: 12

### Only the low bits of the mask are used

```java
writeOutput( bitMaskSet( 255, 15, 0, 4 ) )
```

Result: 255

### Start must be between 0 and 31

```java
try {
	bitMaskSet( 1, 1, 40, 1 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Start must be in the range 0-31, inclusive.
