### Find a substring position with find()

Finds the position of a substring in a StringBuilder.

```java
sb = sb{'Hello BoxLang'};
result = find( sb, 'Box' );
writeOutput( result );
```

Result: 7

### Case-insensitive find with a start position

```java
sb = sb{'Hello BoxLang Box'};
result = sb.findNoCase( 'box', 8 );
writeOutput( result );
```

Result: 15
