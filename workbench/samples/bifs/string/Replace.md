### Replace uppercase 'U' with lowercase 'u'

Replace in Script Syntax

<a href="https://try.boxlang.io/?code=eJxLTy0JS8xRsFUoSi3ISUxO1VBQcs7PSXELLc7Mz1PSUVAKBRGlSgqa1lzlRZklqS6luQUaCukQbUBBAJ5AE5E%3D" target="_blank">Run Example</a>

```java
getVal = replace( "Boxlang", "U", "u" );
writeDump( getVal );

```

Result: Boxlang

### Replace uppercase 'O' with lowercase 'o', but only once

Something similar in Tag Syntax


```java
<bx:set getVal1 = replace( "Boxlang", "O", "o", "ONE" ) >
<bx:dump var="#getVal1#"/>
```

Result: Boxlang

### Example using Callback Function

You can pass in a callback function to the third argument of the `replace` function

<a href="https://try.boxlang.io/?code=eJwtjcEKwjAMhs%2F2KX562mD4AuJheB34DLFUKdSsZAkyxHe3pbskfEm%2BP67YI6eAp3HQtDKs3GiLA2beUUg1Ck8d1i21i06rpBdGfN1JoppUr2uHgvHifk7iZllxhcSSKdS1n%2FEmBqFya4GY8tlP8MS19ueV5mXxLeMjSePdtJgOOOLq%2BA%2FPgDrf" target="_blank">Run Example</a>

```java

public function upCase( Any pattern, Any position, Any orig ) {
	return uCase( pattern );
}
result = replace( "A man a plan a canal.", "an", upCase, "ALL" );
writeOutput( result );

```

Result: A mAN a plAN a cANal.

### Example with start argument (Replace lowercase 'o' with uppercase 'O' from the third position)

You can pass position to start searching in the string

<a href="https://try.boxlang.io/?code=eJxLTy0JSi1WsFUoSi3ISUxO1VBQ8skvS1Vwzs9JcSstzszPU9JRUMoHEf4gwtHHB0QZKyloWnOVF2WWpPqXlhSUlmgopENMAgoDADjeGJA%3D" target="_blank">Run Example</a>

```java
getRes = replace( "Love Boxlang", "o", "O", "ALL", "3" );
writeOutput( getRes );

```

Result: Love Boxlang

### Additional Examples


```java
writeDump( replace( "xxabcxxabcxx", "abc", "def" ) );
writeDump( replace( "xxabcxxabcxx", "abc", "def", "All" ) );
writeDump( replace( "abc", "a", "b", "all" ) );
writeDump( replace( "a.b.c.d", ".", "-", "all" ) );
test = "camelcase CaMeLcAsE CAMELCASE";
test2 = Replace( test, "camelcase", "CamelCase", "all" );
writeDump( test2 );
replacer = ( Any find, Any index, Any input ) => {
	dump( var=arguments, label="replacement arguments" );
	return "-#index#-";
};
writeDump( var=replace( "one string, two strings, three strings", "string", replacer, "all" ), label="replace with a function" );
writeDump( var=replace( "one string, two strings, three strings", {
	"one" : 1,
	"two" : 2,
	"three" : 3,
	"string" : "txt",
	"text" : "string"
} ), label="replace via a struct" );
 // struct keys need to be quoted

```


