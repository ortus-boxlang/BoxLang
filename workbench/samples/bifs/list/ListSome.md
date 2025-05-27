### List contains some.

Take a string list and see if some elements match a given predicate.


```java
var fruitList = arrayToList( [ 
	"apple",
	"mango",
	"orange",
	"pear"
], "," );
writeOutput( listSome( fruitList, ( Any fruit ) => {
	return findNoCase( "n", fruit );
}, "," ) );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyljUsKwkAQRNfOKYpZTaAgBwgjCC7deYKJdCAwiTIfg4p3t5UsdeWmqS6K9%2BKYCzwsGcieJ1I42M60LfqQBUVyMUmybg46PZ4ncYiaCIfdfMM1xCpo4Ld4mE2SUtO8ll65QVlPKt8SQ4hZiJLqetF0ZkljkX2dLg5vjTZ%2F2O6%2FbJ%2Fnu%2B4Fi7lMtw%3D%3D" target="_blank">Run Example</a>

```java
list = ",,a,,b,c,,e,f";
// base test
res = ListSome( list, ( Any value ) => {
	return value == "a";
}, ",", false, true, true );
writeDump( res );
res = ListSome( list, ( Any value ) => {
	return value == "z";
}, ",", false, true, false );
writeDump( res );

```


