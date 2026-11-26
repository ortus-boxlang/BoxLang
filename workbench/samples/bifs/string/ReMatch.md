### Find every match

```java
matches = reMatch( "[0-9]+", "a1b22c333" )
writeOutput( arrayToList( matches ) )

```

Result: 1,22,333

### Case-insensitive matches

```java
string = "1234AbcacAcdd"
writeOutput( arrayToList( reMatchNoCase( "[A-Z]+", string ) ) )

```

Result: AbcacAcdd

### No matches

```java
writeOutput( arrayLen( reMatch( "x", "abc" ) ) )

```

Result: 0

### Member function

```java
writeOutput( arrayToList( "a1b2".reMatch( "[0-9]" ) ) )

```

Result: 1,2
