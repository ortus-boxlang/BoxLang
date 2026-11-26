### Reduce a string from right to left

The characters are processed in reverse order, and the index is the position within the reversed string.

```java
myString = "abcd"
newString = stringReduceRight( myString, ( prev, next, idx ) -> prev & next & idx, "" )
writeOutput( newString )

```

Result: d1c2b3a4

### Reverse a string

```java
writeOutput( stringReduceRight( "abc", ( acc, ch ) -> acc & ch, "" ) )

```

Result: cba

### Member function

```java
writeOutput( "12345".stringReduceRight( ( acc, num ) -> acc + num, 0 ) )

```

Result: 15
