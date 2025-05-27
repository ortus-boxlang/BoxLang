### Script Syntax



<a href="https://try.boxlang.io/?code=eJxLTy0JS8xRsFUoSi3ISUxO9ct3TixO1VBQcs7PSXELLc7Mz1PSUVAqBRGJSgqa1lzlRZklqS6luQUaCukQzUBBADcDFdY%3D" target="_blank">Run Example</a>

```java
getVal = replaceNoCase( "ColdFUsion", "u", "a" );
writeDump( getVal );

```

Result: Expected Result: ColdFasion

### Tag Syntax




```java
<bx:set getVal = replaceNoCase( "ColdFusiOn", "O", "a", "ALL" ) >
<bx:dump var="#getVal#">
```

Result: Expected Result: CaldFusian

### Additional Examples


```java
writeDump( replaceNoCase( "xxabcxxabcxx", "ABC", "def" ) );
writeDump( replaceNoCase( "xxabcxxabcxx", "abc", "def", "All" ) );
writeDump( replaceNoCase( "xxabcxxabcxx", "AbC", "def", "hans" ) );
writeDump( replaceNoCase( "a.b.c.d", ".", "-", "all" ) );
test = "camelcase CaMeLcAsE CAMELCASE";
test2 = replaceNoCase( test, "camelcase", "CamelCase", "all" );
writeDump( test2 );
writeDump( var=replaceNoCase( "One string, two strings, Three strings", {
	"one" : 1,
	"Two" : 2,
	"three" : 3,
	"string" : "txt",
	"text" : "string"
} ), label="replaceNoCase via a struct" );
 // struct keys need to be quoted

```


