### Output current Locale than set it to swiss locale




```java
writeOutput( getlocale() )
writeOutput( " → " )
setLocale( "de_CH" )
writeOutput( getlocale() )

```

Result: English (US) → German (Swiss)

### Additional Examples


```java
var n = 1234.56
writeOutput( getlocale() )
dump( dateTimeFormat( now() ) )
dump( LSdateTimeFormat( now() ) )
dump( numberFormat( n ) )
dump( LSnumberFormat( n ) )
writeOutput( " To " )
setLocale( "french(switzerland)" )
writeOutput( getlocale() )
dump( dateTimeFormat( now() ) )
dump( LSdateTimeFormat( now() ) )
dump( numberFormat( n ) )
dump( LSnumberFormat( n ) )
writeOutput( " To " )
setLocale( "German" )
writeOutput( getlocale() )
dump( dateTimeFormat( now() ) )
dump( LSdateTimeFormat( now() ) )
dump( numberFormat( n ) )
dump( LSnumberFormat( n ) )

```



### Set a locale by common name

`setLocale()` accepts common names, aliases and ISO codes. `getLocale()` returns the alias when one exists.

```java
setLocale( "de_DE" )
writeOutput( getLocale() )
```

Result: German (Standard)
