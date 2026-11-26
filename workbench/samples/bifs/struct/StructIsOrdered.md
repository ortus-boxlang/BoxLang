### Check if a struct is ordered

```java
writeOutput( structNew( "ordered" ).isOrdered() )
```

Result: true

### A default struct is not ordered

```java
s = { a : 1, b : 2 }
writeOutput( structIsOrdered( s ) )
```

Result: false

### Ordered struct literal

The square bracket literal with key-value pairs creates an ordered struct.

```java
s = [ a : 1, b : 2 ]
writeOutput( structIsOrdered( s ) )
```

Result: true
