### jsStringFormat Example

This example illustrates use of the JSStringFormat function.

<a href="https://try.boxlang.io/?code=eJwrLinKzEtXsFVQcsxTSK1IzC3ISVUohgiWJeaUpiqUZ5ZkKCgpFZbml6SmKCkpqJekVpSoK3GVF2WWpPqXlhSUlmgoeAUHg%2FW45RflJgL5UBM0FTQBcgIhTQ%3D%3D" target="_blank">Run Example</a>

```java
string = "An example string value with ""quoted"" 'text'"
writeOutput( JSStringFormat( string ) )

```

Result: An example string value with \"quoted\" \'text\'

### Quotes in a value

```java
str = "Bob's comment was ""BL rocks!"""
writeOutput( str & "<br>" )
writeOutput( jsStringFormat( str ) )

```

Result: Bob's comment was "BL rocks!"<br>Bob\'s comment was \"BL rocks!\"

### Escape line breaks and backslashes

```java
str = "line1" & char( 10 ) & "C:\temp"
writeOutput( jsStringFormat( str ) )

```

Result: line1\nC:\\temp

### Member function

```java
str = "it's"
writeOutput( str.jsFormat() )

```

Result: it\'s
