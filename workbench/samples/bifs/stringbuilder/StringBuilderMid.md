### Extract middle text with mid()

Returns a middle segment from a StringBuilder.

```java
sb = sb{'The quick brown fox'}
result = sb.mid( 5, 5 )
writeOutput( result )
```

Result: quick

### Headless usage

```java
sb = sb{'BoxLang'}
writeOutput( mid( sb, 4, 4 ) )
```

Result: Lang

### Omit count to read to the end

```java
sb = sb{'The quick brown fox'}
writeOutput( sb.mid( 11 ) )
```

Result: brown fox
