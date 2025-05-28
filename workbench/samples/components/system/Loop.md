### For Loop (Script Syntax)

General Purpose Loop

<a href="https://try.boxlang.io/?code=eJxLyy%2FSUMhUsFUwtAZSNkDaAMjQ1lbQVKjm4iwvyixJ9S8tKSgtAanStOaq5QIARYkNGA%3D%3D" target="_blank">Run Example</a>

```java
for( i = 1; i <= 10; i++ ) {
	writeOutput( i );
}

```

Result: 12345678910

### For Loop using Bx:loop Tag

General Purpose Loop


```java
<bx:loop index="i" from="1" to="10">
    <bx:output>#i#</bx:output>
</bx:loop>
```

Result: 1 2 3 4 5 6 7 8 9 10

### Loop Over an Array (Script Syntax)

Array Loop

<a href="https://try.boxlang.io/?code=eJxlkMGqwjAQRdfmKy5dSEul2uVTK6goCML7AHER64gBk0hI0SL%2Bu5Pap09cZEImN%2BcM0fXUOVmjwAaiE8mox3XX1DIS25Ho97G0Dmtrz5jVUGZPVxy4M1%2F%2BZINsAGn2ONkLOcHdGIpR%2BYi3cQEZ0GsyMXSrSfgmTZHgJjoXpzz9Vv5c%2BVdgww%2B3nBL3IGZfcK9MK8vTp6OsnCPjV80syrzpX9iP5D9sM9lClsc4gSa9I4dDZUqvbHDlLGqZGYUQYkxNDTqRZlqvOTx%2FIkEx%2BbK2OXQRYcir%2BxfmAUJ5AJJxb5I%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	"a",
	"b",
	"c"
];
// For Loop By index
for( i = 1; i <= arrayLen( myArray ); i++ ) {
	writeOutput( myArray[ i ] );
}
// By For
for( currentIndex in myArray ) {
	writeOutput( currentIndex );
}
// By arrayEach()
myArray.each( ( Any element, Any index ) => {
	writeOutput( element & " : " & index );
} );

```


### Bx:loop over an Array

Array Loop


```java
<bx:set myArray = [ 
	"a",
	"b",
	"c"
	] > 
 <!--- By index ---> 
 <bx:loop index="i" from="1" to="#arrayLen( myArray )#"> 
 <bx:output>#myArray[ i ]#</bx:output> 
 </bx:loop> 
 <!--- By array ---> 
 <bx:loop index="currentIndex" item="currentItem" array="#myArray#"> 
 <bx:output>#currentIndex#</bx:output> 
 
 <bx:output>#currentItem#</bx:output> 
 </bx:loop>
```


### Loop over a Struct (Script Syntax)

Struct Loop

<a href="https://try.boxlang.io/?code=eJx1jk0LgkAQhs%2FOrxh2LwqS9%2FwAA7tEddBbdBDbaFHX2HaLRfzv6ZJkhy7DzPC8H63JldSVwhh7BOeQ7jNcIyk6YYgPTl6khX1sm07yS0lgCCEIcGPwYXVw7aSLlZaSCbVjBrnAdvb0sAfnJbliR63uWrlIooYn9IvT0ZvO%2FGnpc6ZRMLIEvRCGn8isrG6uB7NqxaYbXUyFwZoZ3y7PstFsLBAnfzrUn3ALLrOm8QYmylXE" target="_blank">Run Example</a>

```java
myStruct = { 
	NAME : "Tony",
	STATE : "Florida"
};
// By struct
for( currentKey in myStruct ) {
	writeOutput( "<li>#currentKey# : #myStruct[ currentKey ]#</li>" );
}
// By structEach()
myStruct.each( ( Any key, Any value ) => {
	writeOutput( "<li>#key# : #value#</li>" );
} );

```


### Bx:loop over a Struct

Loop over a Struct using the collection and item arguments of bx:loop.


```java
<!--- Define our struct ---> 
 <bx:set myStruct = {
	NAME : "Tony",
	STATE : "Florida"
	} > 
 <!--- By struct ---> 
 <bx:loop item="currentKey" collection="#myStruct#"> 
 <bx:output><li>#currentKey# : #myStruct[ currentKey ]#</li></bx:output> 
 </bx:loop>
```


### Bx:loop over a Struct

Loop over a Struct using the collection, index and item arguments of bx:loop.


```java
<!--- Define our struct --->
<bx:set myStruct = {
	NAME : "Tony",
	STATE : "Florida"
	} >
<!--- By struct --->
<bx:loop item="currentItem" collection="#myStruct#" index="currentKey">

<bx:output><li>#currentKey# : #currentItem#</li></bx:output>
</bx:loop>
```


### Loop over a List (Script Syntax)

List Loop


```java
// Define our list
myList = "a, b, c";
// By array
for( item in listToArray( myList, "," ) ) {
	writeOutput( item );
}
// By listEach()
myList.each( ( Any element, Any index ) => {
	writeOutput( element & " : " & index );
}, "," );

```


### Bx:loop over a List

List Loop


```java
<!--- Define our list ---> 
 <bx:set myList = "a, b, c" > 
 <!--- By list ---> 
 <bx:loop index="item" list="#myList#"> 
 <bx:output>#item#</bx:output> 
 </bx:loop> 
 <!--- By array ---> 
 <bx:loop index="currentIndex" array="#listToArray( myList, "," )#"> 
 <bx:output>#currentIndex#</bx:output> 
 </bx:loop>
```


### Loop over a Query with Grouping (Script Syntax)

Query Loop use grouping


```java
q = queryNew( "pk,fk,data", "integer,integer,varchar", [ 
	[
		1,
		10,
		"aa"
	],
	[
		2,
		20,
		"bb"
	],
	[
		3,
		20,
		"cc"
	],
	[
		4,
		30,
		"dd"
	],
	[
		5,
		30,
		"ee"
	],
	[
		6,
		30,
		"ff"
	]
] );
bx:loop query=q group="fk" {
	writeOutput( "<strong>#fk#</strong><br />" );
	bx:loop {
		writeOutput( "&emsp;#pk#:#data#<br />" );
	}
	writeOutput( "<hr>" );
}

```


### Bx:loop over a Query

Query Loop


```java
<!--- Define our query ---> 
 <bx:set platform = [
	"Adobe ColdFusion",
	"Railo",
	"Boxlang"
	] > 
 <bx:set myQuery = queryNew( " " ) > 
 <bx:set queryAddColumn( myQuery, "platform", "VARCHAR", platform ) > 
 <!--- By row index ---> 
 <bx:loop index="i" from="1" to="#myQuery.RECORDCOUNT#"> 
 <bx:output><li>#myQuery[ "platform" ][ i ]#</li></bx:output> 
 </bx:loop> 
 <!--- By group ---> 
 <bx:loop query="myQuery" group="platform"> 
 <bx:output><li>#platform#</li></bx:output> 
 </bx:loop>
```


### While Loop (Script Syntax)

Pre-Condition Loop This form of loop evaluates a single condition at the beginning of each iteration, and continues to loop whilst the condition is true


```java
while (condition) {
	// statements

}

```


### Do While Loop (Script Syntax)

Post-Condition Loop This form of loop evaluates a single condition at the beginning of each iteration, and continues to loop whilst the condition is true


```java
do {
	// statements

}
 while (condition);

```


