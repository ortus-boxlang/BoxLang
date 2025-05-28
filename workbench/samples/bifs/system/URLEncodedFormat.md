### Simple urlEncodedFormat Example

It returns url encoded string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQCA3ycc1Lzk9JTXHLL8pNBIoohWRkFisAUaJCcUlRZl66QnlmSYZCcUFqcmZijkJyRmJRYnJJapGekoKmgqY1FwB4fhsa" target="_blank">Run Example</a>

```java
writeOutput( URLEncodedFormat( "This is a string with special character." ) );

```

Result: This%20is%20string%20with%20special%20character%2E

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjcsKwjAQRff5iqEQaBfpSBQXFhdCm5UrwbVIEmqhbUo60d93DAVhHnA5nJvi%2BFgpDnMPZyheRMt6QnT%2BXY%2FJel%2BH2CPhx482TF5RUDlWDKDeHQ9FI1yalhLut2s32%2BC8MyFOTyoh%2Fc0VVI0ARMh%2Bub9IbXhYInWXhfy5iTPi3dqkbinw2YA24%2BbXKr7VkDpv" target="_blank">Run Example</a>

```java
url_string = "https://dev.boxlang.org/t/welcome-to-boxlang-dev/2064";
dump( URLEncodedFormat( url_string ) );
 // https%3A%2F%2Fdev%2Eboxlang%2Eorg%2Ft%2Fwelcome%2Dto%2Dboxlang%2Ddev%2F2064

```


