### Clear the current locale setting

```java
setLocale( "en_US" )
clearLocale()
writeOutput( "locale cleared" )

```

Result: locale cleared

### Return to the default locale

After clearing, `getLocale()` returns the runtime default locale again.

```java
setLocale( "German (Standard)" )
writeOutput( getLocale() & " → " )
clearLocale()
writeOutput( getLocale() )
```

Result: German (Standard) → English (US) (the default locale of the runtime configuration)
