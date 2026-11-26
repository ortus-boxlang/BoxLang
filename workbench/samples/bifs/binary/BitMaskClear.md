### Bitwise mask clear

Clears 1 bit starting at bit 0 of the number `3` (`11` in binary).

```java
writeOutput( bitMaskClear( 3, 0, 1 ) )
```

Result: 2

### Using a non zero start parameter

Clears the bit at position 1.

```java
writeOutput( bitMaskClear( 3, 1, 1 ) )
```

Result: 1

### Using non zero start and length parameters

Clears 2 bits starting at bit 1 of `10` (`1010` in binary), leaving `1000`.

```java
writeOutput( bitMaskClear( 10, 1, 2 ) )
```

Result: 8

### Clear the low nibble

```java
writeOutput( bitMaskClear( 255, 0, 4 ) )
```

Result: 240

### Start must be between 0 and 31

```java
try {
	bitMaskClear( 255, 32, 1 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Start must be in the range 0-31, inclusive.
