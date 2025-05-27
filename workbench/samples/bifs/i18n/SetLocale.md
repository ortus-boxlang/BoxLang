### Set Locale Script Example

Outputs the current locale, Sets the locale to French (Belgian) and outputs it, then puts it back to the original and outputs it


```java
<bx:script>
	currentLocale = getLocale();
	writeOutput( "Current: " );
	writeDump( currentLocale );
	writeOutput( "<br />" );
	setLocale( "French (Belgian)" );
	writeOutput( "New: " );
	writeDump( getLocale() );
	writeOutput( "<br />" );
	setLocale( currentLocale );
	writeOutput( "Original: " );
	writeDump( getLocale() );
</bx:script>

```

Result: 

### Additional Examples


```java
dump( getLocale() );
setLocale( "english (australian)" );
dump( getLocale() );
dump( Server.COLDFUSION.SUPPORTEDLOCALES.listToArray().sort( "text" ) );

```


