### Append options to config struct (without overwrite flag)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctVz5BSWZ%2BXnFIFmIoCFQ0hlEAyWLS4pKk0scCwpS81I0FJLB5ugoQLXoKKQl5hSnKmhylRdllqT6l5YUlJZoKHgF%2B%2FsFpxZlJuZkVqXCNCloKmgCANMYJ%2F4%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
}
options = {
	B : 1,
	C : 1
}
structAppend( config, options, false )
writeOutput( JSONSerialize( config ) )

```

Result: {"A":0,"B":0,"C":1}

### Append options to config struct (same, but using member function)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctVz5BSWZ%2BXnFIFmIoCFQ0hlEAyWTwTr1EgsKUvNSNBSganUU0hJzilMVNLnKizJLUv1LSwpKSzQUvIL9%2FYJTizITczKrUjUUIHoVNBU0AbFwJVs%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
}
options = {
	B : 1,
	C : 1
}
config.append( options, false )
writeOutput( JSONSerialize( config ) )

```

Result: {"A":0,"B":0,"C":1}

### Append options to config struct (with overwrite flag)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctVz5BSWZ%2BXnFIFmIoCFQ0hlEAyWLS4pKk0scCwpS81I0FJLB5ugowLRocpUXZZak%2BpeWFJSWaCh4Bfv7BacWZSbmZFalwlQraCpoAgDCQyWn" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
}
options = {
	B : 1,
	C : 1
}
structAppend( config, options )
writeOutput( JSONSerialize( config ) )

```

Result: {"A":0,"B":1,"C":1}

### Creating a request context from form and url scopes

Demonstrates how to construct a Request Context (rc) that combines the values of the form and url scopes

<a href="https://try.boxlang.io/?code=eJwrSlawVaiu5SouKSpNLnEsKEjNS9FQKErWUUjLL8pV0MQiUVqUAxQvL8osSfUvLSkoLdFQ8Ar29wtOLcpMzMmsSgWpUtBU0AQAkpEeYw%3D%3D" target="_blank">Run Example</a>

```java
rc = {}
structAppend( rc, form )
structAppend( rc, url )
writeOutput( JSONSerialize( rc ) )

```

Result: a struct containing the combined keys of the form and url scopes (contents depend on the current request)


### Polyfill for earlier versions

In older Boxlang version where this function is not supported yet, you can fall back to a native java method to achieve the same behavior except that it does not have the `overwriteFlag`.

<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctVz5BSWZ%2BXnFIFmIoCFQ0hlEAyWTwTr1CkpLHHNyNBRgajW5yosyS1L9S0uAMhoKXsH%2BfsGpRZmJOZlVqRoKEE0KmgqaALH%2FIv4%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
}
options = {
	B : 1,
	C : 1
}
config.putAll( options )
writeOutput( JSONSerialize( config ) )

```

Result: {"A":0,"B":0,"C":1}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdj08LgkAQxc%2Ftp3jsSUHwHngQg%2BhUYNB5cgeUdFfWNQ%2FRd89%2FaXabN%2B%2FNzG9IFxWVDSK8IHbJ%2BYY9ZGWMDMTucjoOyhT6IcVbhCHS3HTIWmtZO9A0Kg5tVXso6c5lJJOtKQM8yUazgj8sSSyTYxA0d3NO9GU8VgNIzxFfRw423Xw5rmvWCi5nrFlnFojU2TZzU8r7doOfrL%2Fwr2YvCp2VreIGGbntJ%2FHM3BUuH1yQUqz%2BH%2FoAOANn9A%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Create a new animal
newAnimal = {
	CAT : "meow"
}
// Append the newAnimal to animals
StructAppend( animals, newAnimal )
// Show animals, now includes cat
Dump( label="Animals with cat added", var=animals )

```

Result: the first dump shows COW and PIG; the second dump shows COW, PIG and CAT


