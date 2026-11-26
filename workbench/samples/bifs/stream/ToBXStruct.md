### Collect a stream of Map.Entry into a Struct

Converts a stream of entries into a BoxLang Struct.

```java
foods = [ "apples": "healthy", "bananas": "healthy", "pizza": "junk", "tacos": "junk" ]
healthy = foods.entrySet().stream()
    .filter( ( e ) -> e.getValue() == "healthy" )
    .toBXStruct()
writeOutput( healthy.len() & " " & healthy.keyExists( "apples" ) )

```

Result: 2 true

### Create an ordered struct from a stream

```java
letters = [ "z": 26, "a": 1 ]
s = letters.entrySet().stream().toBXStruct( "ordered" )
writeOutput( s.keyList() )

```

Result: z,a

### Create a sorted struct from a stream

```java
letters = [ "d": 4, "c": 3, "b": 2, "a": 1 ]
s = letters.entrySet().stream().toBXStruct( "sorted" )
writeOutput( s.keyList() )

```

Result: a,b,c,d

### Error: unknown struct type

```java
try {
    [ "a": 1 ].entrySet().stream().toBXStruct( "bogus" )
} catch ( any e ) {
    writeOutput( e.message )
}

```

Result: Could not create a struct with a type of [BOGUS] as it is not a known type.
