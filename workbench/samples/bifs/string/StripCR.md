### Removing carriage returns from a string

```java
x = "A" & char( 13 ) & "B" & char( 13 )
y = stripCR( x )
writeOutput( len( x ) & " " & len( y ) & " " & y )

```

Result: 4 2 AB

### Line feeds are not removed

```java
text = "a" & char( 13 ) & char( 10 ) & "b"
writeOutput( len( text ) & " " & len( stripCR( text ) ) )

```

Result: 4 3

### Member function

```java
str = "I love boxlang" & char( 13 )
writeOutput( len( str.stripCR() ) )

```

Result: 14
