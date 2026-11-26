### Right justify a string

`rJustify()` pads the string with spaces on the left so the text is aligned to the right of the field.

```java
writeOutput( "|" & rJustify( "abc", 6 ) & "|" )

```

Result: |   abc|

### Left justify a string

`lJustify()` pads the string with spaces on the right so the text is aligned to the left of the field.

```java
writeOutput( "|" & lJustify( "abc", 6 ) & "|" )

```

Result: |abc   |

### A string longer than the length is not changed

```java
writeOutput( rJustify( "abcdef", 3 ) )

```

Result: abcdef

### Member functions

```java
writeOutput( "|" & "abc".lJustify( 5 ) & "|" & "abc".rJustify( 5 ) & "|" )

```

Result: |abc  |  abc|

### Error: invalid length

```java
try {
	rJustify( "abc", 0 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Length must be greater than 0
