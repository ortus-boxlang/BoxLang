### Bitwise mask read

Reads 1 bit starting at bit 0 of the number `3` (`11` in binary).

```java
writeOutput( bitMaskRead( 3, 0, 1 ) )
```

Result: 1

### Using a non zero start parameter

Reads the bit at position 2 of `10` (`1010` in binary).

```java
writeOutput( bitMaskRead( 10, 2, 1 ) )
```

Result: 0

### Using non zero start and length parameters

Reads 3 bits starting at bit 1 of `10` (`1010` in binary), which are `101`.

```java
writeOutput( bitMaskRead( 10, 1, 3 ) )
```

Result: 5

### Read the high nibble of a byte

```java
writeOutput( bitMaskRead( 171, 4, 4 ) )
```

Result: 10

### Length must be between 0 and 31

```java
try {
	bitMaskRead( 255, 0, 32 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Length must be in the range 0-31, inclusive.
