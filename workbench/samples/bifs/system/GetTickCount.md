### Get the current tick count

Outputs the current value of the internal millisecond timer. The value is a number that depends on the current time.

```java
writeOutput( getTickCount() )
```

Result: a number of milliseconds since the epoch (changes on every call)

### A simple timer

Outputs the millisecond difference between a starting point and end point.

```java
start = getTickCount()
sleep( 1000 )
writeOutput( getTickCount() - start >= 1000 )
```

Result: true

### Choose the unit

Valid units are nano, milli (default) and second.

```java
writeOutput( getTickCount( "second" ) > 0 )
```

Result: true

### Invalid unit

```java
try {
    getTickCount( "bogus" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Invalid unit [bogus] for getTickCount(). Valid units are: nano, milli, second.
