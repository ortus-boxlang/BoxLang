### Calculate bitwise logical OR

Each bit of the result is 1 when the corresponding bit of either number is 1. `5` is `101` and `3` is `011` in binary.

```java
writeOutput( bitOr( 5, 3 ) )
```

Result: 7

### Combine flags

```java
READ = 1
WRITE = 2
writeOutput( bitOr( READ, WRITE ) )
```

Result: 3

### OR with zero

Anything ORed with 0 is unchanged.

```java
writeOutput( bitOr( 10, 0 ) )
```

Result: 10
