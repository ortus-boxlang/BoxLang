### Convert an array to a struct using arrayToStruct()



<a href="https://try.boxlang.io/?code=eJzzCvb3C04tykzMyaxK1VBILCpKrAzJDy4pKk0u0VCIVuDiVEpU0gGSSUpcsQqaCprWXACxsA4w" target="_blank">Run Example</a>

```java
JSONSerialize( arrayToStruct( [ 
	"a",
	"b"
] ) );

```

Result: {"1":"a","2":"b"}

### Additional Examples


```java
arr = [ 
	"a",
	"b",
	"c",
	"d",
	"e",
	"f",
	"g"
];
dump( arrayToStruct( arr ) );
if( listFirst( server.BOXLANG.VERSION, "." ) >= 6 ) dump( arrayToStruct( arr, true ) );
// member function
dump( arr.toStruct() );

```


<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDirObi5PRz9HVVsFJQSlTi4qzVQRVLwiKWjEUsBYtYKkiMK9aaK6U0t0BDIbGoKLEyJD%2B4pKg0uQTMVdBU0LTm0tdXyE3NTUotUkgrzUsuyczPQ2jQK4GpBysFAEnqLQ0%3D" target="_blank">Run Example</a>

```java
arr = [ 
	{
		NAME : "a"
	},
	{
		NAME : "b"
	},
	{
		NAME : "c"
	},
	{
		NAME : "d"
	},
	{
		NAME : "e"
	}
];
dump( arrayToStruct( arr ) );
// member function
dump( arr.toStruct() );

```


