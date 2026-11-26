### Wrap a sentence at a limit

The text is broken at the last space within the limit. The example replaces the line breaks with a pipe so the lines are visible. Lines are joined with the line separator of the operating system.

```java
writeOutput( replace( wrap( "The quick brown fox jumps", 10 ), char( 10 ), "|", "all" ) )

```

Result: The quick|brown fox|jumps

### Words longer than the limit are broken

```java
writeOutput( replace( wrap( "This is a standard sentence.", 7 ), char( 10 ), "|", "all" ) )

```

Result: This is|a|standar|d|sentenc|e.

### Strip the existing line endings first

```java
writeOutput( wrap( "one" & char( 10 ) & "two", 20, true ) )

```

Result: one two

### Member function

```java
writeOutput( replace( "The quick brown fox jumps".wrap( 10 ), char( 10 ), "|", "all" ) )

```

Result: The quick|brown fox|jumps
