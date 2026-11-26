### character and value of Korean currency symbol ( unicode number 8361 )



<a href="https://try.boxlang.io/?code=eJyrULBVSM5ILNJQsDA2M1TQ5CovyixJzS8tKSgt0VBQAkklJpekFlkpKFcoKwHlK4AaEouTMzM1FCowlNskFdmF5mUm56ekKpQl5pSmwrQBAGQFH60%3D" target="_blank">Run Example</a>

```java
x = char( 8361 )
writeoutput( "character: #x#" )
x = ascii( x )
writeoutput( "<br>Unicode value: #x#" )

```

Result: character: ₩<br>Unicode value: 8361

### Get the code of a character

```java
writeOutput( ascii( "A" ) )

```

Result: 65

### Only the first character is used

```java
writeOutput( ascii( "Hello" ) )

```

Result: 72

### Member function

```java
writeOutput( "a".ascii() )

```

Result: 97

### Error: empty string

```java
try {
	ascii( "" )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Input string must contain a single character
