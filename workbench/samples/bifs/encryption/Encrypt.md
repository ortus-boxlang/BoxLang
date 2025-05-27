### Encrypt using AES Encryption in ECB Mode

The key must be generated using the generateSecretKey("AES") function.

<a href="https://try.boxlang.io/?code=eJxLzUsuqiwo0VBQKskvUChOTS5KLVHSUVAKDym0qIpMjkpLDA8r881Lzkz3KCwPtLUFyTm6BoMop8TiVDMTJQVNay4APgQVPg%3D%3D" target="_blank">Run Example</a>

```java
encrypt( "top secret", "WTq8zYcZfaWVvMncigHqwQ==", "AES", "Base64" );

```

Result: keciULin7bxOWvN/BOarWw==

### Encrypt using AES Cipher Block Chaining (CBC) mode

By default encrypt() uses the Electronic Code Book (ECB) mode for encryption.
 For increased security you should specify the mode and padding to use. In this example we will use CBC mode and PKCS5Padding. The value of the encrypted string will be different every time it runs because the IV is generated at random.

<a href="https://try.boxlang.io/?code=eJzLLU5XsFVQSkksSVQoyVdIzUsuqiwoUbLmyk6tBEqkp%2BalFiWWpAanJhellninVmooKDm6BispaFpzAdX6gnVDNWko5Ban6ygANeqAFek7OznrB3g7B5sGJKakZOalKwHFPVwjwJrLizJLUv1LSwpKgfqgJgGFAcD1LEQ%3D" target="_blank">Run Example</a>

```java
msg = "data to encrypt";
key = generateSecretKey( "AES" );
encMsg = encrypt( msg, key, "AES/CBC/PKCS5Padding", "HEX" );
writeOutput( encMsg );

```

Result: 

### Encrypt using AES Galois/Counter Mode (GCM)

Using GCM mode works CF2016+ after update 2. It does not currently work on Lucee (bug: LDEV-904)


```java
msg = "data to encrypt";
key = generateSecretKey( "AES" );
encMsg = encrypt( msg, key, "AES/GCM/NoPadding", "Base64" );
writeOutput( encMsg );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLTq1UsFVIT81LLUosSQ1OTS5KLfFOrdRQUHJ0DVZS0LTmKkktLnHNSy6qLCgBqkyFsIDyxYlpqfmlRSVFqalKOgrZqZU6ED1AKimxONXMBKy7vCizJNWlNLdAQwHZIKAMAKC9J0k%3D" target="_blank">Run Example</a>

```java
key = generateSecretKey( "AES" );
testEncrypt = encrypt( "safeourtree", key, "AES", "base64" );
writeDump( testEncrypt );

```


