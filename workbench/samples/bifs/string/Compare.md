### Case-sensitive comparison

Uppercase letters sort before lowercase letters, so "Boxlang" is less than "boxlang".

```java
writeOutput( compare( "Boxlang", "boxlang" ) )

```

Result: -1

### Equal and greater than

```java
writeOutput( compare( "abc", "abc" ) & " " & compare( "b", "a" ) )

```

Result: 0 1

### Compare strings lexicographically

```java
writeOutput( compare( "100 Main St.", "675 East Avenue" ) & " " & compare( "500 Main St.", "1 Cupertino Way" ) )

```

Result: -1 1

### Member function

```java
writeOutput( "apple".compare( "banana" ) )

```

Result: -1
