### Output current Locale than set it to swiss locale




```java
writeOutput( getlocale() );
writeOutput( " → " );
setLocale( "de_ch" );
writeOutput( getlocale() );

```

Result: english (us) → german (swiss)

### Additional Examples


```java
var n = 1234.56;
writeOutput( getlocale() );
dump( dateTimeFormat( now() ) );
dump( LSdateTimeFormat( now() ) );
dump( numberFormat( n ) );
dump( LSnumberFormat( n ) );
writeOutput( " To " );
setLocale( "french(switzerland)" );
writeOutput( getlocale() );
dump( dateTimeFormat( now() ) );
dump( LSdateTimeFormat( now() ) );
dump( numberFormat( n ) );
dump( LSnumberFormat( n ) );
writeOutput( " To " );
setLocale( "German" );
writeOutput( getlocale() );
dump( dateTimeFormat( now() ) );
dump( LSdateTimeFormat( now() ) );
dump( numberFormat( n ) );
dump( LSnumberFormat( n ) );

```


