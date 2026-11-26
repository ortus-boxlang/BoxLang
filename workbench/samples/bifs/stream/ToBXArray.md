### Collect a stream into a BoxLang Array

Converts a Java Stream into a BoxLang Array by collecting all elements.

```java
arr = [ 10, 20, 30 ].stream().toBXArray()
writeOutput( arr.len() & " " & arr[ 1 ] )

```

Result: 3 10

### Filter a stream then collect to Array

Chain stream operations before collecting.

```java
arr = [ 1, 2, 3, 4, 5 ].stream()
    .filter( ( x ) -> x > 2 )
    .toBXArray()
writeOutput( arrayToList( arr ) )

```

Result: 3,4,5

### Collect a primitive stream

```java
import java.util.stream.IntStream

arr = IntStream.range( 1, 6 ).toBXArray()
writeOutput( arrayToList( arr ) )

```

Result: 1,2,3,4,5
