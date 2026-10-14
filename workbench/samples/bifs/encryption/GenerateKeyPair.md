### Generate an Ed25519 key pair

Returns the private key as base64 PKCS8 DER and the public key as base64 X.509 SPKI DER, plus PEM versions of both.

```java
kp = generateKeyPair( "Ed25519" );
println( kp.publicKey );
println( kp.publicPem );
```

### Additional Examples

```java
// RSA with a 4096 bit key
kp = generateKeyPair( algorithm="RSA", keySize=4096 );

// EC with a named curve
kp = generateKeyPair( algorithm="EC", curve="secp384r1" );
```

Supported algorithms: Ed25519, Ed448, X25519, X448, RSA (2048, 3072, 4096) and EC (secp256r1, secp384r1, secp521r1).
Store the private key securely. Never log it.
