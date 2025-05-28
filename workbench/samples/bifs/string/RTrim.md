### Right Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFMoCinKzNVQUFIAAmc3l%2FzkYhBLSUETKKdko2TNBQCtxQgJ" target="_blank">Run Example</a>

```java
">" & rTrim( "    BLDocs    " ) & "<";

```

Result: >    BLDocs<

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjrEOwjAMRPd%2BxZGhageUD0BsDMwIidk0Lo2UNpXjUD4fmgGGsp19undnLTphUsaTxNM9MBavAwhJxU8PxB7KL4UOpBgoITC51aDJQYV8WI80U8ep6mPEEQZnDiHiFiW4HWAOlbWIWees35pqEa98yuPcwOwNaqzhuuj2X6Ds%2Bvw2pRvQ5Sp%2BbAqv%2FRHf9MJK4Q%3D%3D" target="_blank">Run Example</a>

```java
// create variable with a string of text that has leading and trailing spaces
foo = " Hello World!  ";
// output variable
writeDump( "-" & foo & "-" );
// output variable without trailing spaces
writeDump( "-" & RTrim( foo ) & "-" );

```


