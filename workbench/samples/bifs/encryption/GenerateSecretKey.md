### Generate an AES 128 bit Key

Generate an AES key and use it to encrypt and decrypt a secret.

<a href="https://try.boxlang.io/?code=eJxLrVCwVaiu5Uqt0PN2jQSy01PzUosSS1KDU5OLUku8Uys1FJQcXYOVFDRBaoJdnYNcQ4DKlEryCxSKwWqUQBKufs5BkQEhri5AudS85KLKghINBbgGHQWI%2BToQs4CUU2JxqpkJ1FQXV4TmlFSEZrih%2BPSXF2WWpLqU5haAtChoAgBmjDyC" target="_blank">Run Example</a>

```java
ex = {}
ex.KEY = generateSecretKey( "AES" )
ex.SECRET = "top secret"
ex.ENCRYPTED = encrypt( ex.SECRET, ex.KEY, "AES", "Base64" )
ex.DECRYPTED = decrypt( ex.ENCRYPTED, ex.KEY, "AES", "Base64" )
writeDump( ex )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLrVCwVaiu5Uqt0HP0cfcHcpSCnE2UQPwwR59QV5CAqamJoZERWMzbNRIo4p6al1qUWJIanJpclFrinVqpoQDTrwlS5ernDFTlmpdcVFlQApYDm6WjADFBB1W1iytItUsqQjVQPza1KaW5BSB5BU0AJ%2F4zQg%3D%3D" target="_blank">Run Example</a>

```java
ex = {}
ex.ALGO = "RC4"
ex.VALUE = "554122"
ex.KEY = GenerateSecretKey( ex.ALGO )
ex.ENC = Encrypt( ex.VALUE, ex.KEY, ex.ALGO )
ex.DEC = Decrypt( ex.ENC, ex.KEY, ex.ALGO )
dump( ex )

```



### Choose the key size

The key is returned as a Base64 string. A 128 bit key is 24 characters, and a 256 bit key is 44 characters.

```java
writeOutput( generateSecretKey( "AES", 128 ).len() & " " & generateSecretKey( "AES", 256 ).len() )
```

Result: 24 44

### Each call generates a new random key

```java
writeOutput( generateSecretKey( "AES" ) == generateSecretKey( "AES" ) )
```

Result: false
