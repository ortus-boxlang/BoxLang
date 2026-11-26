### Wrap a string for delayed evaluation

The string is wrapped in double quotes.

```java
writeOutput( de( "pete" ) )
```

Result: "pete" (including the double quotes)

### Embedded quotes are doubled

```java
writeOutput( de( 'say "hi"' ) )
```

Result: "say ""hi"""
