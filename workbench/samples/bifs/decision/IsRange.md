### Check a range created with the range operator

```java
numbers = 1..5
writeOutput( isRange( numbers ) )
```

Result: true

### An exclusive range is still a range

```java
writeOutput( isRange( 1..<10 ) )
```

Result: true

### Arrays and strings are not ranges

```java
writeOutput( isRange( [ 1, 2, 3, 4, 5 ] ) & " " & isRange( "1..5" ) )
```

Result: false false
