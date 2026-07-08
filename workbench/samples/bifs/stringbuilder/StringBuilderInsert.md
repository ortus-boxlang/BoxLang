### Insert text with stringBuilderInsert()

Inserts text at a 1-based position.

```java
sb = sb{'Hello World'};
stringBuilderInsert( sb, 7, 'Beautiful ' );
writeOutput( sb.toString() );
```

Result: Hello Beautiful World

### Member usage

```java
sb = sb{'HelloWorld'};
sb.insert( 6, ' ' );
writeOutput( sb.toString() );
```

Result: Hello World
