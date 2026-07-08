### Trim whitespace with stringBuilderTrim()

Trims leading and trailing whitespace in place and returns the same instance.

```java
sb = sb{'  hello  '};
stringBuilderTrim( sb );
writeOutput( sb.toString() );
```

Result: hello

### Member usage

```java
sb = sb{'  hello  '};
sb.trim();
writeOutput( sb.toString() );
```

Result: hello
