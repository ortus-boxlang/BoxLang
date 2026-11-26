### Encrypt and Decrypt a Secret

Generate an AES 128 bit key and then use it to encrypt and decrypt a secret.

<a href="https://try.boxlang.io/?code=eJxLrVCwVaiu5Uqt0PN2jQSy01PzUosSS1KDU5OLUku8Uys1FJQcXYOVFDRBaoJdnYNcQ4DKlEryCxSKwWqUQBKufs5BkQEhri5AudS85KLKghINBbgGHQWI%2BToQs4CUU2JxqpkJ1FQXV4TmlFSEZrih%2BPSXF2WWpLqU5haAtChoAgBmjDyC" target="_blank">Run Example</a>

```java
ex = {}
ex.KEY = generateSecretKey( "AES" )
ex.SECRET = "top secret"
ex.ENCRYPTED = encrypt( ex.SECRET, ex.KEY, "AES", "Base64" )
ex.DECRYPTED = decrypt( ex.ENCRYPTED, ex.KEY, "AES", "Base64" )
writeDump( ex )

```

Result: a struct where DECRYPTED is `top secret`


### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLTq1UsFVIT81LLUosSQ1OTS5KLfFOrdRQUHLy8Q938wz2UFLQ5CpJLS5xzUsuqiwoAapOhbCAaooT01Lj80uL4kuKUlOVdBSyUyt1kHQC2UmJxalmJjAzXFJhZqSkQs1AMhq%2F%2FvKizJJUl9LcAogemFGaABcUPrc%3D" target="_blank">Run Example</a>

```java
key = generateSecretKey( "BLOWFISH" )
testEncrypt = encrypt( "safe_our_tree", key, "BLOWFISH", "base64" )
testDecrypt = decrypt( testEncrypt, key, "BLOWFISH", "base64" )
writeDump( testDecrypt )

```

Result: safe_our_tree

### Decrypt a known value

```java
writeOutput( decrypt( "keciULin7bxOWvN/BOarWw==", "WTq8zYcZfaWVvMncigHqwQ==", "AES", "Base64" ) )
```

Result: top secret

### Decrypt using CBC mode with an explicit initialization vector

The same IV must be supplied to `encrypt()` and `decrypt()`. A string IV is padded or truncated to 16 bytes.

```java
key = generateSecretKey( "AES" )
iv = "0123456789abcdef"
encrypted = encrypt( "data", key, "AES/CBC/PKCS5Padding", "Base64", iv )
writeOutput( decrypt( encrypted, key, "AES/CBC/PKCS5Padding", "Base64", iv ) )
```

Result: data
