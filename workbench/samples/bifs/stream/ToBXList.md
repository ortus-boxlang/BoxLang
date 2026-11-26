### Collect a stream into a delimited list

Converts a Java Stream into a comma-delimited string.

```java
list = [ "a", "b", "c" ].stream().toBXList()
writeOutput( list )

```

Result: a,b,c

### Use a custom delimiter

```java
list = [ "x", "y", "z" ].stream().toBXList( "|" )
writeOutput( list )

```

Result: x|y|z

### Map then collect to list

```java
list = [ 1, 2, 3 ].stream()
    .map( ( x ) -> "item-" & x )
    .toBXList( ", " )
writeOutput( list )

```

Result: item-1, item-2, item-3
