### Check a Set literal and a Set built with setOf()

```java
colors = set{ "red", "green", "blue" }
writeOutput( isBoxSet( colors ) & " " & isBoxSet( setOf( 1, 2, 2, 3 ) ) )
```

Result: true true

### Arrays and structs are not Sets

Use `.toSet()` to convert an array into a Set.

```java
numbers = [ 1, 2, 3 ]
writeOutput( isBoxSet( numbers ) & " " & isBoxSet( { a: 1 } ) & " " & isBoxSet( numbers.toSet() ) )
```

Result: false false true

### Native Java Sets are also Sets

```java
javaSet = createObject( "java", "java.util.HashSet" ).init()
writeOutput( isBoxSet( javaSet ) )
```

Result: true
