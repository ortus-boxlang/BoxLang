### Collect a stream into a BoxLang Array

Converts a Java Stream into a BoxLang Array by collecting all elements.

```java
arr = streamOf( 10, 20, 30 ).toBXArray();
writeOutput( arr.toString() );

```

Result: [10, 20, 30]

### Filter a stream then collect to Array

Chain stream operations before collecting.

```java
arr = [ 1, 2, 3, 4, 5 ].toStream()
    .filter( (x) => x > 2 )
    .toBXArray();
writeOutput( arr.toString() );

```

Result: [3, 4, 5]

### Map stream values then collect to Array

```java
arr = [ 1, 2, 3 ].toStream()
    .map( (x) => x * 10 )
    .toBXArray();
writeOutput( arr.toString() );

```

Result: [10, 20, 30]
