### Prepend text with stringBuilderPrepend()

Adds text to the beginning of a StringBuilder and returns the same instance.

```java
sb = sb{'World'};
stringBuilderPrepend( sb, 'Hello ' );
writeOutput( sb.toString() );
```

Result: Hello World

### Member usage

```java
sb = sb{'World'};
sb.prepend( 'Hello ' );
writeOutput( sb.toString() );
```

Result: Hello World
