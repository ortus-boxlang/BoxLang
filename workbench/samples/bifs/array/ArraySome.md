### Simple Example



<a href="https://try.boxlang.io/?code=eJxFjjFrAzEMhef4V7xoCD4Ivb2QltK1TQsZOpQOIlESQ842skwIJf%2B9vktpB4mnN3yf%2Bh7PKmwCjmBVvrhpv4RiWOHTzYhzPgktW8ohyv81cDykKXHWsE1G7sv1PV7ZtkeUNIhTKfU0cibmplUef%2FglPJ7iBXutwdBh9YBvN1OxqvFW3hVjtfIR7OhBTOjctU1zvGuIhhvenTWYvFXL1Tz8r%2FMRNPoI96B1og4LEIbxNSk4iwr2qcbdvEF%2FAFwqUR0%3D" target="_blank">Run Example</a>

```java
// Create an array
arrayList = [
	"apple",
	"pineapple",
	"mango",
	"apricot"
]
// Match some
result = arraySome( arrayList, ( Any fruit ) => {
	return fruit.startsWith( "a" )
} )
// Print result
writeOutput( (result ? "Some" : "No") & " matches were found!" )

```

Result: Some matches were found!

### Member Function Example



<a href="https://try.boxlang.io/?code=eJxFjr2KAkEQhGPnKeo6kF043PxARS69PzAwkAsabXXAnRl6ehAR391ZPc6gmuoKvqquw7sKm4ADWJXP7n4%2FfDZMsXYj4pSOQq%2FVJR%2Fk%2BfUc9vHuOKnfRCP367oOn2ybA3Lsxankchw4%2F8zJkDdosAhn7LR4Q4vpDBc3UrGi4RFOJGzzytuhATGhddeqyv5RHwwPrDupN%2FkulopV4l%2FXHLSsFYQ30FekFmMQ%2BmGSZJxEBbtYwvalQm%2BYnk4J" target="_blank">Run Example</a>

```java
// Create an array
arrayList = [
	"apple",
	"pineapple",
	"mango",
	"apricot"
]
// Match some
result = arrayList.some( ( Any fruit ) => {
	return fruit.endsWith( "a" )
} )
// Print result
writeOutput( (result ? "Some" : "No") & " matches were found!" )

```

Result: No matches were found!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJydjrEKgzAURWffV1ycDEil7in4DR1Lh6hPKiWxRKWV0n9vTEjr0KnL5SRc7nmG75W1aoHECZSkKs1d1j6bDbcbZp9dSmdqZ33LYOKIoIsaj4PmvdtT69f6%2BBZyZKjMgn5inQdqHwFcGwLygCcllqfZGt%2BClHBaerntIPsYBBUFNOuaLbrZNFM%2FmOgvnT9Kd6O%2F4S%2Fz9Ye5hHgDDVlY1A%3D%3D" target="_blank">Run Example</a>

```java
newArray = [ 
	"a",
	"b",
	"c",
	"b",
	"d",
	"b",
	"e",
	"f"
]
dump( newArray )
hasSome1 = arraySome( newArray, ( Any item, Any idx, Any arr ) => {
	return item == "b"
} )
dump( hasSome1 )
// member function
hasSome2 = newArray.some( ( Any item, Any idx, Any arr ) => {
	return item == "k"
} )
dump( hasSome2 )

```


