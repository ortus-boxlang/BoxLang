### Calculate bitwise logical AND

Each bit of the result is 1 only when the corresponding bits of both numbers are 1. `5` is `101` and `3` is `011` in binary.

```java
writeOutput( bitAnd( 5, 3 ) )
```

Result: 1

### Mask off the low byte of a number

```java
writeOutput( bitAnd( 4660, 255 ) )
```

Result: 52

### Works with 64-bit numbers

```java
writeOutput( bitAnd( 4294967296, 4294967297 ) )
```

Result: 4294967296

### AND with zero

Anything ANDed with 0 is 0.

```java
writeOutput( bitAnd( 5, 0 ) )
```

Result: 0
