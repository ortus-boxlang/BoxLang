### Replace the first occurrence ignoring case

```java
writeOutput( replaceNoCase( "xxabcxxabcxx", "ABC", "def" ) )

```

Result: xxdefxxabcxx

### Replace every occurrence

```java
writeOutput( replaceNoCase( "Boxlang", "O", "a", "all" ) )

```

Result: Baxlang

### No match returns the original string

```java
writeOutput( replaceNoCase( "Boxlang", "u", "a" ) )

```

Result: Boxlang

### Member function

```java
writeOutput( "Hello".replaceNoCase( "L", "_", "all" ) )

```

Result: He__o
