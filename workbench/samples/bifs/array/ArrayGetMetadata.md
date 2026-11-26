### Simple Example



<a href="https://try.boxlang.io/?code=eJxFjL0KAjEQhGv3KZZUdyDmCaxEbRSvsBGxWHTRgJcL6xxyb28Si2uG%2BWE%2B73ljKlCWyGImE1U9hA94zVdaOEnprW6ZXQpR59RLfA6ObuQ97xT3F%2FcK4YdAqEi%2BV9ReccxDqRqe4W35dRYi%2FiVjSkpfC9DTiDSiqajV%2BdJtuf0BbEM2Gw%3D%3D" target="_blank">Run Example</a>

```java
// Create an array
arrayList = [
	"apple",
	"pineapple",
	"mango"
]
// Fetch meta data
data = arrayGetMetadata( arrayList )
// Print array type
writeOutput( data.TYPE )

```

Result: synchronized

### Member Function Example



<a href="https://try.boxlang.io/?code=eJxFTDsKwkAQrZ1TDFslINkTWIlWiilsglgMOsQFsy7jCyG3dzeCNo%2F39563pgJliSxmMtOCh%2FAGb%2FhCKycpPdWtM0sh6l8NEvuXoyt5z3vF7cGDQvguECqQ57%2Brplccc1r8qi6L1kLEt8CYk9JkAXoakUZUy0lz7tod1x8qpjQK" target="_blank">Run Example</a>

```java
// Create an array
arrayList = [
	"apple",
	"pineapple",
	"mango"
]
// Fetch meta data
data = arrayList.getMetadata()
// Print array type
writeOutput( data.TYPE )

```

Result: synchronized

### Metadata fields

The metadata struct contains `type` (synchronized or unsynchronized), `dimensions` and `datatype` (always `any`).

```java
arr = arrayNew( 1, false )
meta = arr.getMetadata()
writeOutput( meta.type & "," & meta.dimensions & "," & meta.datatype )
```

Result: unsynchronized,1,any
