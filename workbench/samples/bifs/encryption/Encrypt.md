### Encrypt using AES Encryption in ECB Mode

The key must be generated using the generateSecretKey("AES") function.

<a href="https://try.boxlang.io/?code=eJxLzUsuqiwo0VBQKskvUChOTS5KLVHSUVAKDym0qIpMjkpLDA8r881Lzkz3KCwPtLUFyTm6BoMop8TiVDMTJQVNABOSFPk%3D" target="_blank">Run Example</a>

```java
encrypt( "top secret", "WTq8zYcZfaWVvMncigHqwQ==", "AES", "Base64" )

```

Result: keciULin7bxOWvN/BOarWw==

### Encrypt using AES Cipher Block Chaining (CBC) mode

By default encrypt() uses the Electronic Code Book (ECB) mode for encryption.
 For increased security you should specify the mode and padding to use. In this example we will use CBC mode and PKCS5Padding. The value of the encrypted string will be different every time it runs because the IV is generated at random.

<a href="https://try.boxlang.io/?code=eJzLLU5XsFVQSkksSVQoyVdIzUsuqiwoUeLKTq0Eiqen5qUWJZakBqcmF6WWeKdWaigoOboGKylocgFV%2BoL1QrVoKOQWp%2BsoAPXpgNXoOzs56wd4OwebBiSmpGTmpSsBxT1cI0B6y4syS1L9S0sKSoHaoAZpAgD3BStO" target="_blank">Run Example</a>

```java
msg = "data to encrypt"
key = generateSecretKey( "AES" )
encMsg = encrypt( msg, key, "AES/CBC/PKCS5Padding", "HEX" )
writeOutput( encMsg )

```

Result: a hex string which is different on every run


### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLTq1UsFVIT81LLUosSQ1OTS5KLfFOrdRQUHJ0DVZS0OQqSS0ucc1LLqosKAEqTIWwgNLFiWmp%2BaVFJUWpqUo6CtmplToQLUAqKbE41cwEpLm8KLMk1aU0t0BDAdkcTQAQEyaO" target="_blank">Run Example</a>

```java
key = generateSecretKey( "AES" )
testEncrypt = encrypt( "safeourtree", key, "AES", "base64" )
writeDump( testEncrypt )

```

Result: a Base64 string which is different on every run

### Unknown encodings throw an error

```java
key = generateSecretKey( "AES" )
try {
	encrypt( "top secret", key, "AES", "bogus" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The encoding argument [bogus] is not a valid encoding type

### The default encoding is UU

```java
key = generateSecretKey( "AES" )
encrypted = encrypt( "top secret", key )
writeOutput( decrypt( encrypted, key ) )
```

Result: top secret
