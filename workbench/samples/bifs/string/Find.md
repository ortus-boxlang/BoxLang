### Script Syntax



<a href="https://try.boxlang.io?code=eJxLy8xL8ct3TixO1VBQKlbSUVBKyq%2FIScxL18svKiktTsrPzy7WS87PBcoYKGhaAwB9TA9L" target="_blank">Run Example</a>

```java
findNoCase( "s", "boxlang.ortusbooks.com", 0 )

```

Result: 13

### Case-sensitive find

`find()` returns the 1-based position of the first match, or 0 if there is no match.

```java
writeOutput( find( "o", "boxlang" ) & " " & find( "B", "boxlang" ) )

```

Result: 2 0

### Start from a position

```java
writeOutput( find( "o", "boxlang.ortus", 3 ) )

```

Result: 9

### Case-insensitive find

```java
writeOutput( findNoCase( "Tree", "Grow the tree, Save the world" ) )

```

Result: 10

### Member functions

```java
str = "I love boxlang"
writeOutput( str.find( "boxlang" ) & " " & str.findNoCase( "BOX" ) )

```

Result: 8 8
