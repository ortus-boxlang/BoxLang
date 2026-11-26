### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKEp1y8xL8ct3TixO1VBQclXSUVAqSS0uUTA0MlZUUtBU0AQAiOsOIA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( reFindNoCase( "E", "test 123!" ) )

```

Result: 2

### Script Syntax

Example with all optional arguments

<a href="https://try.boxlang.io/?code=eJzzCvb3C04tykzMyaxK1VAoSnXLzEvxy3dOLAbylFyVdBSUSlKLSxQMjYwVgRxDHYWSotJUoKijj4%2BSgqaCJgCd%2FBI4" target="_blank">Run Example</a>

```java
JSONSerialize( reFindNoCase( "E", "test 123!", 1, true, "ALL" ) )

```

Result: [{"len":[1],"pos":[2],"match":["e"]}]

### Find the first match

By default the 1-based position of the first match is returned, or 0 if there is no match.

```java
writeOutput( reFind( "[0-9]+", "abc 123 def 45" ) )

```

Result: 5

### Start from a position

```java
writeOutput( reFind( "[0-9]+", "abc 123 def 45", 8 ) )

```

Result: 13

### Return the position of every match

```java
writeOutput( jsonSerialize( reFind( "[0-9]+", "abc 123 def 45", 1, false, "all" ) ) )

```

Result: [5,13]

### No match

```java
writeOutput( reFind( "[0-9]+", "abc" ) )

```

Result: 0

### Member function

```java
writeOutput( "abc 123".reFind( "[0-9]+" ) )

```

Result: 5
