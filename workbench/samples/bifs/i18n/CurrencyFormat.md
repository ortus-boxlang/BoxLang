### Tag Example

lsCurrencyFormat returns a currency value using the locale convention. Default value is local. 


```java
<!--- loop through list of locales; show currency values for 100,000 units --->
<bx:loop LIST="#Server.BOXLANG.SUPPORTEDLOCALES#" index="locale" delimiters=",">
<bx:set oldlocale = setLocale( locale ) >
<bx:output><p><b><I>#locale#</I></b>
Local: #lsCurrencyFormat( 100000, "local" )#
International: #lsCurrencyFormat( 100000, "international" )#
None: #lsCurrencyFormat( 100000, "none" )#
<hr noshade>
</bx:output>
</bx:loop>
```


### Script Example using specific locale

lsCurrencyFormat returns a currency value using the specified locale.

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMgpdi4tKkrNS650yy%2FKTSzRUDDVUVDKyU9OzFECMlLz4kODlRQ0FTQB7FcQsQ%3D%3D" target="_blank">Run Example</a>

```java
writeDump( lsCurrencyFormat( 5, "local", "en_US" ) )

```

Result: $5.00


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzM0vzStRsFUwNDDgyslPTsxJBXKUUvPiQ4OVuMqLMktS80tLCkpLNBR8gp1Li4pS85Ir3fKLchOBIolgzToKSmCNSjoKUAM0FTStFfT1FVSAhuoBzSXWmMy8ktSivMSSzPw8bMaFBrsokGhiXn5eKqZBUEMQ3s1KjPcKoNi77%2FcvNaSeb70CIhVIMQ7Dq1xQrwIAIMmcCg%3D%3D" target="_blank">Run Example</a>

```java
amount = 100
locale = "en_US"
writeoutput( LSCurrencyFormat( amount, "local", locale ) ); // $100.00
writeoutput( LSCurrencyFormat( amount, "international", locale ) ); // USD 100.00
writeoutput( LSCurrencyFormat( amount, "none", locale ) ); // 100.00
locale = "ja_JP"
writeoutput( LSCurrencyFormat( amount, "local", locale ) ); // ￥100
writeoutput( LSCurrencyFormat( amount, "international", locale ) ); // JPY 100
writeoutput( LSCurrencyFormat( amount, "none", locale ) )
 // 100

```



### The type argument controls the currency symbol

```java
writeOutput( currencyFormat( 100, "local", "en_US" ) & " / " & currencyFormat( 100, "international", "en_US" ) & " / " & currencyFormat( 100, "none", "en_US" ) )
```

Result: $100.00 / USD 100.00 / 100.00

### Using the member function

```java
writeOutput( (5).currencyFormat( "local", "en_US" ) )
```

Result: $5.00

### Invalid types throw an error

```java
try {
	currencyFormat( 100, "bogus", "en_US" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The argument [type] [bogus] is not recognized as a valid currency format type.
