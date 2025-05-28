### SHA-256 Hash Example

Returns 64 character hex result.

<a href="https://try.boxlang.io/?code=eJzLSCzO0FBQKs7PTS3JyMxLV9JRUAr2cNQ1MjUDMUND3HQtlBQ0rbkAAfEKpw%3D%3D" target="_blank">Run Example</a>

```java
hash( "something", "SHA-256", "UTF-8" );

```

Result: 3fc9b689459d738f8c88a3a48aa9e33542016b7a4052e001aaa536fca74813cb

### MD5 Hash Example

MD5 is not recommended for use requiring security.

<a href="https://try.boxlang.io/?code=eJzLSCzO0FBQKs7PTS3JyMxLV1LQtOYCAE8ZBo0%3D" target="_blank">Run Example</a>

```java
hash( "something" );

```

Result: 437b930db84b8079c2dd804a71936b5f

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1zLsOgjAYQOGdp%2FjDBIOUQqEQ4sClxsVV52IrNCmXlJLo24sa4%2BR8Tj6E4MyN4q2WOy3HzvawWKPGboHZTGK9SripuxTf2POll0vgqHFeLezBPUqtJ7hMRgu3cMQ6zN578uCz%2BOAXgBBUOCyzpsIpYSFNCA6TipZ5XjEasfjAkp944uaxEQI4aGWtlqD50AZ%2FdefF12Wekk2ntEkITVmJMWMNZjiLaR2TyHkC1LhEYQ%3D%3D" target="_blank">Run Example</a>

```java
// Variable-length strings produce fixed-length hashes.
input = "Hello World";
dump( hash( input ) ); // B10A8DB164E0754105B7A99BE72E3FE5
input = "Mary had a little lamb.";
dump( hash( input ) );
 // CA964B1677D5476EA11EED1E1837C342

```



```java
// Different algorithms are supported.
input = "Hello World";
dump( hash( input ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input, "QUICK" ) ); // 6b736ba1c9a95606
dump( hash( input, "MD5" ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input, "bxmX_COMPAT" ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input, "SHA" ) ); // 0A4D55A8D778E5022FAB701977C5D840BBC486D0
dump( hash( input, "SHA-256" ) ); // A591A6D40BF420404A011733CFB7B190D62C65BF0BCDA32B57B277D9AD9F146E
dump( hash( input, "SHA-384" ) ); // 99514329186B2F6AE4A1329E7EE6C610A729636335174AC6B740F9028396FCC803D0E93863A7C3D90F86BEEE782F4F3F
dump( hash( input, "SHA-512" ) ); // 2C74FD17EDAFD80E8447B0D46741EE243B7EB74DD2149A0AB1B9246FB30382F27E853D8585719E0E67CBDA0DAA8F51671064615D645AE27ACB15BFB1447F459B
// The default, MD5 and bxmX_COMPAT algorithms are all MD5.
dump( hash( input ) == hash( input, "MD5" ) && hash( input, "MD5" ) == hash( input, "bxmX_COMPAT" ) );
 // true

```


<a href="https://try.boxlang.io/?code=eJy1kVFPwjAQx5%2FZp7j0CQKGdmOMMWqySQ0%2BGB808bmDwhq7jXRd1Bi%2Bu900C0YS4cGX9tL73%2F%2Fud5XFvjZAAa2EUiU8l1ptUOSMx%2FCUCSjqPBUayi1IIzQ3siwqWGe82AkwNp%2FxKnM2db7vt2EfZGNH23PUVN91ZZTAAAYRWOeE4Hi2TMh0wnDgTwj2kyAOw4QFLvNumX%2BuY9g5%2BozceHhyuwy9MEkY8YIZW4ZBwNx46U4Tv%2BH5UQtKVJVF4AUQ4FpAJQyYEsi5vbHtTekl1EbXopnjvrTtjtZp%2BIuAvHk0Mm8VBGMMj6v4yiduNbIDdmoQfJ3Nwc8rJ32bN3oN5n0vKJKFkoVAoHgqFEV%2FWCD4cHrbUtvR7d%2FjyF6Ltq2NhkM7sk33TrBxtSu1NFlO0bc5OgEcOb2Dc3BerVI81MZW9gEtUn2NmtwJQHy8jy9El1zM%2BMvlPylxh%2FkJW0D9hg%3D%3D" target="_blank">Run Example</a>

```java
input = "Hello World";
// The number of iterations change the hash
dump( hash( input=input, numIterations=1 ) ); // B10A8DB164E0754105B7A99BE72E3FE5
dump( hash( input=input, numIterations=9 ) ); // 5E1C304FD939BBE1378ED977E2AD26B5
// numIterations less than 1 are set to 1
dump( hash( input=input, numIterations=0 ) == hash( input=input, numIterations=1 ) ); // true
// More iterations take more time
// 1000 SHA-512s, 1 iteration each: 5ms
bx:timer type="inline" label="1000 SHA-512s, 1 iteration each" {
	for( i = 0; i < 1000; i++ ) {
		hash( input=input, algorithm="SHA-512", numIterations=1 );
	}
}
writeOutput( "<br>" );
// 1000 SHA-512s, 10 iterations each: 21ms
bx:timer type="inline" label="1000 SHA-512s, 10 iterations each" {
	for( i = 0; i < 1000; i++ ) {
		hash( input=input, algorithm="SHA-512", numIterations=10 );
	}
}

```


