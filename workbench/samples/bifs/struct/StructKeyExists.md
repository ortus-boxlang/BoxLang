### Check if server struct has OS key



<a href="https://try.boxlang.io/?code=eJwrLikqTS7xTq10rcgsLinWUChOLSpLLdJRUMovVlLQtOYCANfYCy4%3D" target="_blank">Run Example</a>

```java
structKeyExists( server, "os" );

```

Result: true

### Check if server struct has OS key using member function

CF11+ calling the keyExists member function on a struct.

<a href="https://try.boxlang.io/?code=eJwrTi0qSy3Sy06tdK3ILC4p1lBQyi9WUtC05gIAhJEIiw%3D%3D" target="_blank">Run Example</a>

```java
server.keyExists( "os" );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjs0KgkAUhdfOUxxmo4HgPmlRFiFBRQmth%2BGKgzoTjlEivnuOCUXczf04PxyhVS0qixV6MC853bAEr43hIfPO6d6RUbp0mKyzSSTzdLhJL1vHslDNnbMhZlGEpCBZojWwRFA5SupAL2VbC6Vh2%2BYhW6byANfpPVC3m9QA4jMkBLdaqIpjMV7PPJKFCcCzgpqx0UJg0l2dP2f80RyzgYEqSz%2BZo5m93wX%2FkTfpU0v8" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow",
	BIRD : "chirp"
};
// Check to see if key exists in struct
if( StructKeyExists( animals, "snail" ) ) {
	echo( "There is a snail in 'animals'" );
}
 else {
	echo( "No snail exists in 'animals'" );
}

```


