### Check if a value is a valid currency

```java
writeOutput( isCurrency( "$100.00" ) )

```

Result: true

### Returns false for non-currency values

```java
writeOutput( isCurrency( "not a currency" ) )

```

Result: false

### Plain numbers are valid currency values

```java
writeOutput( isCurrency( "1234.5" ) & " " & isCurrency( "$1,234.50", "en_US" ) )
```

Result: true true
