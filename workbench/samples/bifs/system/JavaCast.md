### Convert a BoxLang number to a Java double

Converts 180 degrees to radians using the Java method Math.toRadians( double ).

```java
writeOutput( createObject( "java", "java.lang.Math" ).toRadians( javaCast( "double", 180.0 ) ) )
```

Result: 3.141592653589793

### Cast a string to an int

```java
value = javaCast( "int", "42" )
writeOutput( value + 1 )
```

Result: 43

### Values that cannot be cast throw

The cast is strict.

```java
try {
    javaCast( "int", "abc" )
} catch ( any e ) {
    writeOutput( "cast failed" )
}
```

Result: cast failed
