### Delete a range with stringBuilderDelete()

Removes characters using 1-based, inclusive start and end positions.

```java
sb = sb{'Hello World'};
stringBuilderDelete( sb, 6, 11 );
writeOutput( sb.toString() );
```

Result: Hello

### Member usage

```java
sb = sb{'Hello World'};
sb.delete( 6, 11 );
writeOutput( sb.toString() );
```

Result: Hello
