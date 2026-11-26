### Script Syntax



<a href="https://try.boxlang.io/?code=eJxNj80Kg0AMhM%2BbpwieFKTQn1Ohh0LpqaUPIB4Wu9aldVdiViviu3cVlZ6G8M1MEkv6pY384Al7BBFYowI8Yg9CbL0GLAsdgBhiz7i1C9uNjJxcUUFqDe6noCW30Nw6WuBhhG0xJWEAyJ3JWFuDpawe8zEhnk2H73iSBqMxSIodGWwSvOmar5pqDrHZvFU3zmHkXanvy%2FVXPf0zNZPL%2BC6rEO3cGv%2BvwAha0qwurvSWBMTqAjF1QIrRDwSaUSQ%3D" target="_blank">Run Example</a>

```java
original = { 
	"one" : {
		1 : "tahi"
	},
	"two" : {
		2 : "rua"
	},
	"three" : {
		3 : "toru"
	},
	"four" : {
		4 : "wha"
	}
}

function mapOriginal( Any k, Any v ) {
	return v[ ListFirst( v.keyList() ) ]
}
fixed = structMap( original, mapOriginal )
writeDump( [
	original,
	fixed
] )

```

Result: original is unchanged; fixed has the same keys with the values tahi, rua, toru and wha


### Using Member Function

 

<a href="https://try.boxlang.io/?code=eJw9jssKgzAQRdeZrxiySkCEPlYFC4XSVf9AXAQaa7BNyphoRfz3RmlcXYYz9%2BHIPI1VLyxwQmDcWc3xhBMwtovKvWoMBzZnkfnBJbZfGAW1oYb0ZjysRkch0doFSvC4wKFZnTBDbb76Ecvdf0f%2BVh%2BBAi92xDZbpUeJxXkxk%2FaBLPYl3k3nb4Y6L7DPWz0ut5DxsYIZJQxkvL6Gd4wqgaXsOGWtgwrlD4ZNQpw%3D" target="_blank">Run Example</a>

```java
original = { 
	"one" : {
		1 : "tahi"
	},
	"two" : {
		2 : "rua"
	},
	"three" : {
		3 : "toru"
	},
	"four" : {
		4 : "wha"
	}
}
fixed = original.map( ( Any k, Any v ) => {
	return v[ ListFirst( v.keyList() ) ]
} )
writeDump( [
	original,
	fixed
] )

```

Result: original is unchanged; fixed has the same keys with the values tahi, rua, toru and wha


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjsEKwjAMhs%2F2KUIPssFwqDehwvSsF5%2Bg24KUdXW06cYQ392siIKX8MGffPlDQ6AgkI8NXXHKQFrjOmwl5CI0tKk43SY6Me0SnZn2YvKGsI39kMGoveKgAKtrtEo%2BvLkbp%2B3Hu7jKEmodEAgDCY%2BBFbcUXjQL0nEGlZuhw7lIMGobEXJQR3iKlUeK3i0prEEeJM%2B0IF4FsIcX%2Fwvxk2%2BhXg8Dtr86b%2FVeTYE%3D" target="_blank">Run Example</a>

```java
sct = structNew( "linked" )
sct.A = 1
sct.B = 2
sct.C = 3
writedump( var=sct, label="original struct" )
// base test
res = StructMap( sct, ( Any key, Any value ) => {
	return key & ":" & value
}, true )
writedump( var=res, label="mapped struct" )

```

Result: the mapped struct contains the values A:1, B:2 and C:3 (the true argument runs the map in parallel)

### Double every value

```java
prices = { apple : 1, pear : 2 }
doubled = prices.map( ( k, v ) -> v * 2 )
writeOutput( doubled.apple & " " & doubled.pear )
```

Result: 2 4
