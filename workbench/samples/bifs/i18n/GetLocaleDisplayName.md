### Output current Locale's display name than set it to swiss locale




```java
writeOutput( getLocaleDisplayName() )
writeOutput( " → " )
setLocale( "de_CH" )
writeOutput( getLocaleDisplayName() )

```

Result: English (United States) → Deutsch (Schweiz)

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUEhPLfHJT07MSXXJLC7ISaz0S8xN1dBU0LTmAgAJfgzQ" target="_blank">Run Example</a>

```java
writeDump( getLocaleDisplayName() )

```



### Display a locale name in another language

```java
writeOutput( getLocaleDisplayName( "de_DE" ) & " / " & getLocaleDisplayName( "de_DE", "fr_FR" ) )
```

Result: Deutsch (Deutschland) / allemand (Allemagne)
