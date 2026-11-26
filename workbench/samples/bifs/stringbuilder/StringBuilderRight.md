### Get right characters with right()

Returns the rightmost characters from a StringBuilder.

```java
sb = sb{'The quick brown fox'}
result = sb.right( 3 )
writeOutput( result )
```

Result: fox

### Headless usage

```java
sb = sb{'BoxLang'}
writeOutput( right( sb, 4 ) )
```

Result: Lang

### Negative count omits characters from the left

```java
sb = sb{'BoxLang'}
writeOutput( sb.right( -3 ) )
```

Result: Lang
