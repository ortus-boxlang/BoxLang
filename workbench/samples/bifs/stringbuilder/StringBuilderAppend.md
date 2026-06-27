### Append text with stringBuilderAppend()

Appends text to the end of a StringBuilder and returns the same instance.

```java
sb = sb'Hello';
stringBuilderAppend( sb, ' World' );
writeOutput( sb.toString() );
```

Result: Hello World

### Use append() as a member function

```java
result = sb"foo"
    .append( 'bar' )
    .append( 'baz' )
    .toString();
writeOutput( result );
```

Result: foobarbaz
