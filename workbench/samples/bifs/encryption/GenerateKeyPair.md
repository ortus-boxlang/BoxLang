### Generate an Ed25519 key pair

Returns the private key as base64 PKCS8 DER and the public key as base64 X.509 SPKI DER, plus PEM versions of both.

```java
kp = generateKeyPair( "Ed25519" )
println( kp.publicKey )
println( kp.publicPem )
```

Result: the Base64 public key followed by a PEM block that starts with `-----BEGIN PUBLIC KEY-----`

### Additional Examples

```java
// RSA with a 4096 bit key
kp = generateKeyPair( algorithm="RSA", keySize=4096 )

// EC with a named curve
kp = generateKeyPair( algorithm="EC", curve="secp384r1" )
```

Supported algorithms: Ed25519, Ed448, X25519, X448, RSA (2048, 3072, 4096) and EC (secp256r1, secp384r1, secp521r1).
Store the private key securely. Never log it.

### The struct returned

```java
kp = generateKeyPair( "RSA" )
writeOutput( kp.keyList() )
```

Result: algorithm,privateKey,publicKey,privatePem,publicPem

### Invalid argument combinations throw an error

The `keySize` argument only applies to RSA.

```java
try {
	generateKeyPair( "EC", 2048 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The keySize argument only applies to RSA, not [EC].
