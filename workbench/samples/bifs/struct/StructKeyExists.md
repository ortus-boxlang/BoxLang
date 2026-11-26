### Check if server struct has OS key



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKC4pKk0u8U6tdK3ILC4pBgqkFpWlFukoKOUXKyloKmgCAKOPEDY%3D" target="_blank">Run Example</a>

```java
writeOutput( structKeyExists( server, "os" ) )

```

Result: true

### Check if server struct has OS key using member function

Calling the keyExists member function on a struct.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKE4tKkst0stOrXStyCwuKdZQUMovVlLQVNAEAC0sDZM%3D" target="_blank">Run Example</a>

```java
writeOutput( server.keyExists( "os" ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjkELgkAQhc%2Fur3jsRQPBe9ChLEKCihI6LzLhoO6Gu1Ei%2FfdcEyrmMh%2FvvZmnNDeqtlighwjSwwVzyMYYGYvgmG09GdaVx3SZjyKZh8dVdlp7Lkpub1K8RJIgLamo4AwsEfiKijrQk62zYA3r2nvhBF8jnMd1R91mVCOoT48Y0mrFtcRsmF4EVJQmgsxLaoeLFgqj7s%2BFUyYczMN7UG3pJ7I3k%2FVb4D%2FxBs3WS0E%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow",
	BIRD : "chirp"
}
// Check to see if key exists in struct
if( StructKeyExists( animals, "snail" ) ) {
	echo( "There is a snail in 'animals'" )
}
 else {
	echo( "No snail exists in 'animals'" )
}

```

Result: No snail exists in 'animals'

### Keys are case-insensitive

```java
user = { Name : "Luis" }
writeOutput( structKeyExists( user, "name" ) & " " & user.keyExists( "age" ) )
```

Result: true false
