### Format text into paragraphs

Inserts paragraph tags around blocks of text separated by line breaks.

```java
text = "First paragraph.

Second paragraph."
result = paragraphFormat( text )
writeOutput( result.contains( "<p>" ) )

```

Result: true

### Using the member function

```java
result = "Line one.

Line two.".paragraphFormat()
writeOutput( result.contains( "</p>" ) )

```

Result: true

### Each line becomes a paragraph

```java
text = "One" & char( 10 ) & "Two"
writeOutput( replace( paragraphFormat( text ), char( 10 ), "|", "all" ) )

```

Result: <p>One</p>|<p>Two</p>
