### List contains some.

Take a string list and see if some elements match a given predicate.


```java
var fruitList = arrayToList( [ 
	"apple",
	"mango",
	"orange",
	"pear"
], "," )
writeOutput( listSome( fruitList, ( Any fruit ) => {
	return findNoCase( "n", fruit )
}, "," ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyljcEKwjAYg8%2F2KUJPLQT2BBUEj958gk7%2BwaCb0v5VpvjuVtlRT15CEkK%2BNBZFgCUj2fNECgdrug59LAKVoiZLaZNDWx7Pkzik5giH3bzgGlMVeIQtHmaTRWue1zK022jNk%2B3dEkNMRQjNdVV4c8ujyr5OF4c3xP%2BDuv9AfcIX1gtdhksQ" target="_blank">Run Example</a>

```java
list = ",,a,,b,c,,e,f"
// base test
res = ListSome( list, ( Any value ) => {
	return value == "a"
}, ",", false, true, true )
writeDump( res )
res = ListSome( list, ( Any value ) => {
	return value == "z"
}, ",", false, true, false )
writeDump( res )

```

### Member function with a match

```java
writeOutput( "a,b,c".listSome( ( value ) -> value == "b" ) )

```

Result: true

### No item matches

```java
writeOutput( listSome( "a,b,c", ( value ) -> value == "z" ) )

```

Result: false
