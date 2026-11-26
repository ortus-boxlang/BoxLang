### Calculate bitwise logical XOR

Each bit of the result is 1 when the corresponding bits of the two numbers differ. `5` is `101` and `3` is `011` in binary.

```java
writeOutput( bitXor( 5, 3 ) )
```

Result: 6

### XOR a number with itself

```java
writeOutput( bitXor( 128, 128 ) )
```

Result: 0

### Toggle a bit

XORing with a mask flips the masked bits.

```java
writeOutput( bitXor( 5, 1 ) )
```

Result: 4
