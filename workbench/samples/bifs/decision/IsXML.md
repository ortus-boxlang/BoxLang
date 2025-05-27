### isXML Example

Returns true if the string is well-formed XML.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJVbBVULJJzs9JSSstzszPS81Lz8xLLbbjUrCBMIEsBZu8xNxUO8eU%2FKRUBWegUjewUht9sDBQpT5cKYYmn9Lk1FSiVAYlZubkE6XSvyA1T8EppzTVpSgxHas7bPQxfaRkzVVelFmS6l9aUlBaoqGQWRyRm6OhkAoNBk0FTWsuAPAfWi0%3D" target="_blank">Run Example</a>

```java
example = "<coldfusionengines>
 <engine>
  <name>Adobe ColdFusion</name>
 </engine>
 <engine>
  <name>Lucee</name>
 </engine>
 <engine>
  <name>Railo</name>
 </engine>
 <engine>
  <name>Open BlueDragon</name>
 </engine>
</coldfusionengines>";
writeOutput( isXml( example ) );

```

Result: true

### isXML Example for Invalid XML

Returns false if the string is not well-formed XML.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJNVSwVVCySc7PSUkrLc7Mz0vNS8%2FMSy2241KwgTCBLAWbvMTcVDvHlPykVAVnoFI3sFIbfbAwUKU%2BXCmGJp%2FS5NRUqEola67yosySVP%2FSkoLSEg2FzOKI3BwNhVSYSzQVNK25ADTpMcQ%3D" target="_blank">Run Example</a>

```java
example1 = "<coldfusionengines>
 <engine>
  <name>Adobe ColdFusion</name>
 </engine>
 <engine>
  <name>Lucee</name>";
writeOutput( isXml( example1 ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLz8lPSswJSyxSsFVQsnFNLCrJsONSsClKLSzNLEpNAbIVbPISc1PtQopSU4tt9MFsoAJ9JBVYVIcnlqQWEa3aMb0oM7k0p6S0KJVoPR6luYl5mSWVWDXY6EN8omTNVV6UWZLqX1pSUFqioZBZHJGbo6GQDve0poKmNRcAVs9U%2FA%3D%3D" target="_blank">Run Example</a>

```java
globalVar = "<Earth>
 <required>
  <name>Trees</name>
 </required>
 <required>
  <name>Water</name>
 </required>
 <required>
  <name>Agriculture</name>
 </required>
 <required>
  <name>Humanity</name>
 </required>
</Earth>";
writeOutput( isXml( globalVar ) );

```


