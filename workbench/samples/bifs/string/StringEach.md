### Full function



<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUErMKCyvUrLmSk7MyUlKTM4GimkoOOZVKmTmFShoKtjaKVRzcZYXZZak5peWFJSWaIAlbIE6C5UUNK25aq25gkuKMvPSXROTMzQUciAm6yjAzQOqAQCnTSKs" target="_blank">Run Example</a>

```java
letters = "ahqwz";
callback = ( Any inp ) => {
	writeoutput( inp == "q" );
};
StringEach( letters, callback );

```

Result: NONOYESNONO

### Member function




```java
letters = "ahqwz";
letters.each( ( Any inp ) => {
	writeoutput( inp == "q" );
} );

```

Result: NONOYESNONO

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLzCsoLQkuKcrMS1ewVVBKTEpOUbLmKgYLuCYmZ2goZCJU6ChoKDjmVSqUJeYoaCrY2ilUc3GWF2WWpOaXlgAVaYAlbIHGJCspaFpz1YIIADyIHkE%3D" target="_blank">Run Example</a>

```java
inputString = "abcd";
stringEach( inputString, ( Any val ) => {
	writeoutput( val == "c" );
} );

```


