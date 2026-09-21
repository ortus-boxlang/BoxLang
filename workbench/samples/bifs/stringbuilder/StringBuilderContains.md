### Check for a substring with stringBuilderContains()

Checks whether a StringBuilder contains a substring.

```java
sb = sb{'Hello BoxLang'};
result = stringBuilderContains( sb, 'Box' );
writeOutput( result );
```

Result: true

### Case-insensitive contains

```java
sb = sb{'Hello BoxLang'};
result = sb.containsNoCase( 'boxlang' );
writeOutput( result );
```

Result: true
