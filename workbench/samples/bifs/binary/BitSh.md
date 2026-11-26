### Shift right by 1 bit

Uses `bitShrn` to perform a bitwise shift-right operation (no rotation).

```java
writeOutput( bitShrn( 5, 1 ) )
```

Result: 2

### Shift left

Uses `bitShln` to perform a bitwise shift-left operation, which multiplies by powers of two.

```java
writeOutput( bitShln( 5, 2 ) )
```

Result: 20

### Shift right is an unsigned shift

Zeros are shifted in from the left, so negative numbers become large positive numbers.

```java
writeOutput( bitShrn( -1, 28 ) )
```

Result: 15

### Shift left wraps as a 32-bit integer

```java
writeOutput( bitShln( 1, 31 ) )
```

Result: -2147483648
