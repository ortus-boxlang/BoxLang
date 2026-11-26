### Extract month from date

Grabs the month out of a raw date yyyymmdd value.

<a href="https://try.boxlang.io/?code=eJzLzUzRUFAyMjAwMDQ0MlLSUVAyBRFGSgqa1lwAXLcFZA%3D%3D" target="_blank">Run Example</a>

```java
mid( "20001122", "5", "2" )

```

Result: 11

### Extract a number of characters

```java
writeOutput( mid( "I Love Boxlang server", 8, 5 ) )

```

Result: Boxla

### Omit the count to get the rest of the string

```java
writeOutput( mid( "Hello", 3 ) )

```

Result: llo

### Member function

```java
writeOutput( "Hi buddy, Nice day!".mid( 4, 5 ) )

```

Result: buddy

### Start past the end of the string

```java
writeOutput( "[" & mid( "abc", 10, 2 ) & "]" )

```

Result: []
