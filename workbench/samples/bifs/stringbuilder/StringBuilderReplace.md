### Replace a range with stringBuilderReplace()

Replaces characters using 1-based, inclusive start and end positions.

```java
sb = sb{'Hello World'};
stringBuilderReplace( sb, 7, 11, 'BoxLang' );
writeOutput( sb.toString() );
```

Result: Hello BoxLang

### Member usage

```java
sb = sb{'Hello World'};
sb.replace( 7, 11, 'BoxLang' );
writeOutput( sb.toString() );
```

Result: Hello BoxLang
