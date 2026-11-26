### SHA-256 Hash Example

Returns 64 character hex result.

<a href="https://try.boxlang.io/?code=eJzLSCzO0FBQKs7PTS3JyMxLV9JRUAr2cNQ1MjUDMUND3HQtlBQ0AeyeCmI%3D" target="_blank">Run Example</a>

```java
hash( "something", "SHA-256", "UTF-8" )

```

Result: 3fc9b689459d738f8c88a3a48aa9e33542016b7a4052e001aaa536fca74813cb

### MD5 Hash Example

MD5 is not recommended for use requiring security.

<a href="https://try.boxlang.io/?code=eJzLSCzO0FBQKs7PTS3JyMxLV1LQBABCCQZI" target="_blank">Run Example</a>

```java
hash( "something" )

```

Result: 437b930db84b8079c2dd804a71936b5f

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1zLsOgjAYQOGdp%2FjDBINCoaUQ48ClxsVV52IrNCmXtCXRtxc1xsn5nHxRBGduFG%2B13Gg5dq4H64waOwuzmcRylXBTdym%2Bsee2l3brqXFeHOzBP0qtJ7hMRgvfE8swB%2B8ngM8RQriDKIIKxWXeVCjDLKYEo5hUtCyKitGEpQdGfuCJm8dKCOCglXNaguZDu%2F2Dey%2B8LosMrzalDcE0YyVCjDWIoTyldYqTJ8mOQ6Y%3D" target="_blank">Run Example</a>

```java
// Variable-length strings produce fixed-length hashes.
input = "Hello World"
dump( hash( input ) ); // B10A8DB164E0754105B7A99BE72E3FE5
input = "Mary had a little lamb."
dump( hash( input ) )
 // CA964B1677D5476EA11EED1E1837C342

```



```java
// Different algorithms are supported.
input = "Hello World"
dump( hash( input ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input, "QUICK" ) ); // 6b736ba1c9a95606
dump( hash( input, "MD5" ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input, "SHA" ) ); // 0A4D55A8D778E5022FAB701977C5D840BBC486D0
dump( hash( input, "SHA-256" ) ); // A591A6D40BF420404A011733CFB7B190D62C65BF0BCDA32B57B277D9AD9F146E
dump( hash( input, "SHA-384" ) ); // 99514329186B2F6AE4A1329E7EE6C610A729636335174AC6B740F9028396FCC803D0E93863A7C3D90F86BEEE782F4F3F
dump( hash( input, "SHA-512" ) ); // 2C74FD17EDAFD80E8447B0D46741EE243B7EB74DD2149A0AB1B9246FB30382F27E853D8585719E0E67CBDA0DAA8F51671064615D645AE27ACB15BFB1447F459B
// The default and MD5 algorithms are the same.
dump( hash( input ) == hash( input, "MD5" ) )
 // true

```


<a href="https://try.boxlang.io/?code=eJy1kEFPwjAUx8%2F0U7z0BAFDuzHGgJlsrgQPxoMmnjsorLFbSddFjeG72yFBEknUg5f2pe%2F%2Ffs3vyWrXWIgBL4VSGp60UWuMhkN4LARUTZkLA3oD0grDrdRVDauCV1sB1vULXhdo3ZS77qHsgmxp8eEctNO3p7GYQg96M3DklJJkkqV0PGIkDEaUBGmYRFHKQo%2F5Cxb8lhidiAGjNz4ZLbLIj9KUUT%2BcsCwKQ%2BYlmTdOg9bnThtxrmH5s4CyfbSyFG2CEkLgYZlcBdSrB0C%2F0iD4qphCUNYof522eQP2bSdiLCslK4FB8VyoGP%2BAwPCOOhttnJZbOZm5a3741lX9vtNx7c4Fb6622khblDE%2BwvGF9aLOHu3RiwuK%2B8a6wS7geW6usWtd0CPn2%2FgU9OifDb9R%2FtORHCU%2FANeNzuw%3D" target="_blank">Run Example</a>

```java
input = "Hello World"
// The number of iterations change the hash
dump( hash( input=input, numIterations=1 ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input=input, numIterations=9 ) ); // 5E1C304FD939BBE1378ED977E2AD26B5
// More iterations take more time
// 1000 SHA-512s, 1 iteration each: 5ms
bx:timer type="inline" label="1000 SHA-512s, 1 iteration each" {
	for( i = 0; i < 1000; i++ ) {
		hash( input=input, algorithm="SHA-512", numIterations=1 )
	}
}
writeOutput( "<br>" )
// 1000 SHA-512s, 10 iterations each: 21ms
bx:timer type="inline" label="1000 SHA-512s, 10 iterations each" {
	for( i = 0; i < 1000; i++ ) {
		hash( input=input, algorithm="SHA-512", numIterations=10 )
	}
}

```



### Hash40 and the member function

`hash40()` always uses SHA1. Strings also have a `hash()` member function.

```java
writeOutput( hash40( "something" ) & " " & "something".hash( "SHA-256" ) )
```

Result: 1af17e73721dbe0c40011b82ed4bb1a7dbe3ce29 3fc9b689459d738f8c88a3a48aa9e33542016b7a4052e001aaa536fca74813cb

### Unknown algorithms throw an error

```java
try {
	hash( "something", "NOPE" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The algorithm [NOPE] provided is not a valid digest algorithm.
