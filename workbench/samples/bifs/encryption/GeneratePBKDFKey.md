### Example PBKDF2 With HMAC SHA1

The `PBKDF2WithHmacSHA1` algorithm will work on older JVMs, or older versions of CF

<a href="https://try.boxlang.io/?code=eJxLT81LLUosSQ1w8nZx806t1FBQAjONwjNLMjxyE5ODPRwNlXQUlIpTk4tSS8CsxJySSiDD1MDAQEfB0MhCQRMAEMoUNw%3D%3D" target="_blank">Run Example</a>

```java
generatePBKDFKey( "PBKDF2WithHmacSHA1", "secret", "salty", 5000, 128 )

```

Result: Y0MCpCe3zb0CNJvyXNUWEQ==

### More complex encryption example




```java
// some variables
password = "top_secret"
dataToEncrypt = "the most closely guarded secret"
encryptionAlgorithm = "AES"
keysize = 128
algorithmVersion = 512
PBKDFalgorithm = "PBKDF2WithHmacSHA" & algorithmVersion
// Generate key as recommended in docs
length = keysize / 8
multiplicator = 10 ^ length
salt = Round( Randomize( 5, "SHA1PRNG" ) * multiplicator )
// The magic happens here
PBKDFKey = GeneratePBKDFKey( PBKDFalgorithm, password, salt, algorithmVersion, keysize )
encryptedData = encrypt( dataToEncrypt, PBKDFKey, encryptionAlgorithm, "BASE64" )
decryptedData = decrypt( encryptedData, PBKDFKey, encryptionAlgorithm, "BASE64" )
// Output
writeOutput( "<b>Generated PBKDFKey (Base 64)</b>: " & PBKDFKey )
writeOutput( "<br /><b>Data After Encryption</b>: " & encryptedData )
writeOutput( "<br /><b>Data After Decryption</b>: " & decryptedData )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBIT81LLUosSQ1w8nZx806t1FBQAjONwjNLMjxyE5ODPRwNlXQUlIpTk4tSS8CsxJySSiDD1MDAQEfB0MhCQVNBk0tBX18h0sDXucA51bgqycDZz6usMsIvNNw10NYWAM6AHsI%3D" target="_blank">Run Example</a>

```java
dump( generatePBKDFKey( "PBKDF2WithHmacSHA1", "secret", "salty", 5000, 128 ) )
 // Y0MCpCe3zb0CNJvyXNUWEQ==

```



### The same passphrase and salt always produce the same key

The `PBKDF2WithSHA256` alias is the same as `PBKDF2WithHmacSHA256`. The default key size is 256 bits, which is 44 characters of Base64.

```java
a = generatePBKDFKey( "PBKDF2WithHmacSHA256", "my passphrase", "my salt" )
b = generatePBKDFKey( "PBKDF2WithSHA256", "my passphrase", "my salt" )
writeOutput( ( a == b ) & " " & a.len() )
```

Result: true 44
