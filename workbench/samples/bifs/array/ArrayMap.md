### Script Syntax



<a href="https://try.boxlang.io/?code=eJxLzs8tyEmtcEksSVSwVYhW4OKs5uLkdFSwUjDh4qzVQXANLVD5poZAPlesNVdearljUVFiJVB7Ioj2TSzQUEhGGKujoKHgmFepkFmSmqugqWBrpwA0oii1pLQoDyym52jNVaugac1VXgTkupTmArXDzQQKAwAMuCw5" target="_blank">Run Example</a>

```java
complexData = [ 
	{
		A : 4
	},
	{
		A : 18
	},
	{
		A : 51
	}
];
newArray = arrayMap( complexData, ( Any item ) => {
	return item.A;
} );
writeDump( newArray );

```

Result: [4, 18, 51]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1jk0LgkAQhs%2B7v2LYk4IkeUwMvAZ26RgdJh3Jw46xupSE%2F739APsF3R5m5p33wTNqmqCCK0ihGjStnVTm8IIGH4FO40T9wKTkrZSd1c8EMKbSUjK9Au%2FdCzQGlwa3fQYJ1LzAMJPOInFH74juGFKojvCRwtBsDXsSil1SwSGEpFhLufqaWPsrc6M8B036TgZ6y%2B08jLzJFF4m0E57nT9pFH70BQgcX4A%3D" target="_blank">Run Example</a>

```java
aNames = [ 
	"Marcus",
	"Sarah",
	"Josefine"
];
dump( aNames );
newNames1 = arrayMap( aNames, ( Any item, Any index, Any arr ) => {
	return {
		"name" : item
	};
} );
dump( newNames1 );
// member function
newNames2 = aNames.map( ( Any item, Any index, Any arr ) => {
	return {
		"name" : item
	};
} );
dump( newNames2 );

```


