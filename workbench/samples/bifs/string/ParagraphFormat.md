### Format text into paragraphs

Inserts paragraph tags around blocks of text separated by line breaks.

```java
text = "First paragraph.

Second paragraph.";
result = paragraphFormat( text );
writeOutput( result.contains( "<p>" ) );

```

Result: true

### Using the member function

```java
result = "Line one.

Line two.".paragraphFormat();
writeOutput( result.contains( "</p>" ) );

```

Result: true
