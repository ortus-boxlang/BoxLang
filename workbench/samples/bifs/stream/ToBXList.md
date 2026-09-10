### Collect a stream into a delimited list

Converts a Java Stream into a comma-delimited string.

```java
list = streamOf( "a", "b", "c" ).toBXList();
writeOutput( list );

```

Result: a,b,c

### Use a custom delimiter

```java
list = streamOf( "x", "y", "z" ).toBXList( "|" );
writeOutput( list );

```

Result: x|y|z

### Map then collect to list

```java
list = [ 1, 2, 3 ].toStream()
    .map( (x) => "item-" & x )
    .toBXList( ", " );
writeOutput( list );

```

Result: item-1, item-2, item-3
