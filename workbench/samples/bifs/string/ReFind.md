### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrSnXLzEvxy3dOLE7VUFByVdJRUCpJLS5RMDQyVlRS0LTmAgC4MAkY" target="_blank">Run Example</a>

```java
reFindNoCase( "E", "test 123!" )

```

Result: 2

### Script Syntax

Example with all optional arguments

<a href="https://try.boxlang.io/?code=eJzzCvb3C04tykzMyaxK1VAoSnXLzEvxy3dOLAbylFyVdBSUSlKLSxQMjYwVgRxDHYWSotJUoKijj4%2BSgqaCpjUXAMLsEn0%3D" target="_blank">Run Example</a>

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
writeOutput( serializeJSON( reFind( "[0-9]+", "abc 123 def 45", 1, false, "all" ) ) )

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
