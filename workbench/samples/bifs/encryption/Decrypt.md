### Encrypt and Decrypt a Secret

Generate an AES 128 bit key and then use it to encrypt and decrypt a secret.

<a href="https://try.boxlang.io/?code=eJxLrVCwVaiuteZKrdDzdo0EctJT81KLEktSg1OTi1JLvFMrNRSUHF2DlRQ0wYqCXZ2DXEOA6pRK8gsUisGKlMAyrn7OQZEBIa4uQMnUvOSiyoISDQW4Dh0FiA06ENOAlFNicaqZCcxcF1eE7pRUhG64qXgNKC%2FKLEl1Kc0tAOkBCQAA8t497g%3D%3D" target="_blank">Run Example</a>

```java
ex = {};
ex.KEY = generateSecretKey( "AES" );
ex.SECRET = "top secret";
ex.ENCRYPTED = encrypt( ex.SECRET, ex.KEY, "AES", "Base64" );
ex.DECRYPTED = decrypt( ex.ENCRYPTED, ex.KEY, "AES", "Base64" );
writeDump( ex );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLTq1UsFVIT81LLUosSQ1OTS5KLfFOrdRQUHLy8Q938wz2UFLQtOYqSS0ucc1LLqosKAEqT4WwgIqKE9NS4%2FNLi%2BJLilJTlXQUslMrdZC0AtlJicWpZiZwQ1xSYYakpEINQTKbgAHlRZklqS6luQUQTTCzgDIAOTM%2FrQ%3D%3D" target="_blank">Run Example</a>

```java
key = generateSecretKey( "BLOWFISH" );
testEncrypt = encrypt( "safe_our_tree", key, "BLOWFISH", "base64" );
testDecrypt = decrypt( testEncrypt, key, "BLOWFISH", "base64" );
writeDump( testDecrypt );

```


