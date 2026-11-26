### Full function



<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUErMKCyvUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcZYXZZak5peWFJSWaIAlbIE6C5UUNK25aq25gkuKMvPSXROTMzQUciAm6yjAzQOqAQCnTSKs" target="_blank">Run Example</a>

```java
letters = "ahqwz"
callback = ( Any inp ) => {
	writeoutput( inp == "q" )
}
StringEach( letters, callback )

```

Result: falsefalsetruefalsefalse

### Use the index of each character

```java
stringEach( "abc", ( ch, i ) => writeOutput( i & ch & " " ) )

```

Result: 1a 2b 3c 
