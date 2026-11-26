### Tag Syntax

In the following example, the function call requests element number 2 from the string, using the delimiter '[:;".' 


```java
<bx:output>
<bx:set mystring = "four," & char( 32 ) & char( 9 ) & char( 10 ) & ",five, nine,zero:;" & char( 10 ) & "nine,ten:, eleven:;twelve:;thirteen," & char( 32 ) & char( 9 ) & char( 10 ) & ",four" >
getToken(mystring, 3) is : #getToken( mystring, 3 )#
</bx:output>
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFzLEKgCAUBdC9r3g4GbypUWkL%2BoH2MLyEaBr2Kvr7aGo8y5ESkWfBIdTTCJk%2Ba1IPUio3V3jDpbq8gpd0wlhO7kL2qLyHHI1VTB2TMora5q5BMJzbrkn%2Bt30B1Nohlg%3D%3D" target="_blank">Run Example</a>

```java
token_test = GetToken( "yellow,red:,orange,blue:;,lavender,pink:;", 2, ":" )
writeDump( token_test )

```

### Get a token by position

```java
writeOutput( getToken( "one,two,three", 2 ) )

```

Result: two

### Use a custom delimiter

```java
writeOutput( getToken( "a:b:c", 3, ":" ) )

```

Result: c

### Member function

```java
writeOutput( "red,green,blue".getToken( 3 ) )

```

Result: blue

### Index past the last token

```java
result = getToken( "one,two,three", 5 )
writeOutput( "[" & result & "]" )

```

Result: [] (an empty string is returned)
