### New struct using literal notation

Creates an unordered struct.

<a href="https://try.boxlang.io/?code=eJwrzcsvSkktSk1RsFWoVuDidFSwUjDU4eJ0BtLGQNoJSBtx1VpzlRdllqS6lOYWaCiUwvVoWnMBAC8kEWc%3D" target="_blank">Run Example</a>

```java
unordered = { 
	A : 1,
	C : 3,
	B : 2
};
writeDump( unordered );

```


### New struct using function

Creates an unordered struct.

<a href="https://try.boxlang.io/?code=eJwrzcsvSkktSk1RsFUoLikqTS7xSy3X0LTmKoVJ6DkCpQyRBZyAAkbIAs5AAWNrrvKizJJUl9LcAg0FuJwC0CgAniYgjg%3D%3D" target="_blank">Run Example</a>

```java
unordered = structNew();
unordered.A = 1;
unordered.B = 2;
unordered.C = 3;
writeDump( unordered );

```


### New ordered struct using literal notation

CF2016+ Creates an ordered struct. Note the square brackets.

<a href="https://try.boxlang.io/?code=eJzLL0pJLUpNUbBViFbg4nRUsFIw1OHidALSRkDaGUgbc8Vac5UXZZakupTmFmgo5EN1aFpzAQDI2Q9h" target="_blank">Run Example</a>

```java
ordered = [ 
	A : 1,
	B : 2,
	C : 3
];
writeDump( ordered );

```


### New ordered struct using function

Creates an ordered struct.

<a href="https://try.boxlang.io/?code=eJzLL0pJLUpNUbBVKC4pKk0u8Ust11BQyoeIKiloWnNB2XqOQDWGCK4TkGuE4DoDucbWXOVFmSWpLqW5BRoKUBmQEQB8Th%2BI" target="_blank">Run Example</a>

```java
ordered = structNew( "ordered" );
ordered.A = 1;
ordered.B = 2;
ordered.C = 3;
writeDump( ordered );

```


### New ordered struct using literal notation

Creates an ordered struct.


```java
ordered = [];
ordered.A = 1;
ordered.B = 2;
ordered.C = 3;
writeDump( ordered );

```


### New case-sensitive struct using function

CF2021+ Creates a case-sensitive struct.

<a href="https://try.boxlang.io/?code=eJxLTixOLU7NK84sySxLVbBVKC4pKk0u8Ust11BQSkaWU1LQtOZCEdFzBKo3RBd0AgoaoQs6AwWNrbnKizJLUl1Kcws0FFDkQUYDAPspLu4%3D" target="_blank">Run Example</a>

```java
casesensitive = structNew( "casesensitive" );
casesensitive.A = 1;
casesensitive.B = 2;
casesensitive.C = 3;
writeDump( casesensitive );

```


### New case-sensitive struct using literal notation

CF2021+ Creates a case-sensitive struct.


```java
casesensitive = $;
{
	A : 1,
	B : 2,
	C : 3
};
writeDump( casesensitive );

```


### New ordered and case-sensitive struct using function

CF2021+ Creates a case-sensitive struct.

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVKAYz%2FFLLNRSU8otSUotSU3STE4tTi1PzijNLMstSlRQ0rbmK4Vr0nIGajFFEnIAiRigijkARQ2uu8qLMklSX0twCDQWEJMg4AJpRK88%3D" target="_blank">Run Example</a>

```java
someStruct = structNew( "ordered-casesensitive" );
someStruct.C = 3;
someStruct.B = 2;
someStruct.A = 1;
writeDump( someStruct );

```


### New ordered and case-sensitive struct using literal notation

CF2021+ Creates an ordered and case-sensitive struct.


```java
someStruct = $;
[
	C : 3,
	B : 2,
	A : 1
];
writeDump( someStruct );

```


### Additional Examples


```java
dump( var=structNew( "soft" ), label="soft" );
dump( var=structNew( "weak" ), label="weak" );
dump( var=structNew( "linked" ), label="linked" );
st = {
	"one" : [
		1,
		2,
		3
	],
	"two" : {
		"three" : QueryNew( "id" )
	},
	THREE : "unquoted keys don't preserve case"
};
dump( st );
dump( structKeyList( st ) );
dump( structKeyExists( st, "one" ) );
// shorthand syntax for a new empty ordered struct, [=] also works
st = [];
st.C = 1;
st.B = 2;
st.A = 3;
dump( st );
// shorthand syntax for an ordered struct with values
st = [
	C : 1,
	B : 2,
	A : 3
];
dump( st );

```



```java
<bx:set st = structNew() >

<bx:set st[ "name" ] = "John Doe" >
<bx:set st[ "age" ] = 30 >
<bx:set st[ "city" ] = "New York" >

<bx:dump var="#st#">
```


