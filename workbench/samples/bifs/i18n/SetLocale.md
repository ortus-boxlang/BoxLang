### Set Locale Script Example

Outputs the current locale, Sets the locale to French (Belgian) and outputs it, then puts it back to the original and outputs it


```java
<bx:script>
	currentLocale = getLocale()
	writeOutput( "Current: " )
	writeDump( currentLocale )
	writeOutput( "<br />" )
	setLocale( "French (Belgian)" )
	writeOutput( "New: " )
	writeDump( getLocale() )
	writeOutput( "<br />" )
	setLocale( currentLocale )
	writeOutput( "Original: " )
	writeDump( getLocale() )
</bx:script>

```


### Additional Examples


```java
dump( getLocale() )
setLocale( "english (australian)" )
dump( getLocale() )
dump( Server.BOXLANG.SUPPORTEDLOCALES.listToArray().sort( "text" ) )

```



### Set the locale by ISO code

`setLocale()` returns the display name of the locale which was set.

```java
writeOutput( setLocale( "de_DE" ) )
```

Result: German (Germany)

### Invalid locales throw an error

```java
try {
	setLocale( "not_a_locale" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The Locale requested, [not_a_locale], is not a valid Locale for this runtime.
