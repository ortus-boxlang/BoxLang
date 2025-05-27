### Tag Example

lsCurrencyFormat returns a currency value using the locale convention. Default value is local. 


```java
<!--- loop through list of locales; show currency values for 100,000 units --->
<bx:loop LIST="#Server.COLDFUSION.SUPPORTEDLOCALES#" index="locale" delimiters=",">
<bx:set oldlocale = setLocale( locale ) >
<bx:output><p><b><I>#locale#</I></b>
Local: #lsCurrencyFormat( 100000, "local" )#
International: #lsCurrencyFormat( 100000, "international" )#
None: #lsCurrencyFormat( 100000, "none" )#
<hr noshade>
</bx:output>
</bx:loop>
```

Result: 

### Script Example using specific locale

lsCurrencyFormat returns a currency value using the specified locale.

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMgpdi4tKkrNS650yy%2FKTSzRUDDVUVDKyU9OzFECMlLz4kODlRQ0FTStuQAOSBD2" target="_blank">Run Example</a>

```java
writeDump( lsCurrencyFormat( 5, "local", "en_US" ) );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzM0vzStRsFUwNDCw5srJT07MSQXylFLz4kODlay5yosyS1LzS0sKSks0FHyCnUuLilLzkivd8otyE4EiiWDtOgpKYJ1KOgpQEzQVNK0V9PUVVIDG6hkYEG1MZl5JalFeYklmfh4240KDXRRINDEvPy8V0yCoIQj%2FZiXGewVQ7t%2F3%2B5caUs%2B7XgGRCqQYh%2BlXLqhnuQDEqJ0A" target="_blank">Run Example</a>

```java
amount = 100;
locale = "en_US";
writeoutput( LSCurrencyFormat( amount, "local", locale ) ); // $100.00
writeoutput( LSCurrencyFormat( amount, "international", locale ) ); // USD 100.00
writeoutput( LSCurrencyFormat( amount, "none", locale ) ); // 100.00
locale = "ja_JP";
writeoutput( LSCurrencyFormat( amount, "local", locale ) ); // ￥100
writeoutput( LSCurrencyFormat( amount, "international", locale ) ); // JPY 100
writeoutput( LSCurrencyFormat( amount, "none", locale ) );
 // 100

```


