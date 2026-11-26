### Simple Example

Inserts a new key/value into the structure

<a href="https://try.boxlang.io/?code=eJwli1EKgzAQBb%2BbUzz2qwFPIHgAaaFnSHXRgKZhd4OIePcG%2FRoYZtaQ0eGAe9AcohBa0Fd%2BW6KmKt5ZLzUJcyJ3OjUpg%2FVJWeyJNeQGtMSsVCk8ErzbJBp%2FiuVSi7t%2F8f6Oeg%2Fw8H8XWSRd" target="_blank">Run Example</a>

```java
map = { 
	"hair" : "brown",
	"eyes" : "green"
}
structInsert( map, "lips", "red" )
writeOutput( structKeyList( map ) )

```

Result: eyes,lips,hair

### Overwrite Example

Throws exception when you try to add a duplicate key, when allowoverwrite parameter is false.

<a href="https://try.boxlang.io/?code=eJwli0EKgzAQRdfmFL%2BzUvAEhR6gq55hmo5VqDFMJtgg3t3Yrh483ps54oYNrqGRJyVcQU9d1kB9VVIk%2FdRbRQK53ZkWbK5JptnbPSRRazFz7PH%2FK1VeFQN%2FkqBzOzybH9FyKJBvd96rTiaPbDHXmUR10Qud7QGT4iqO" target="_blank">Run Example</a>

```java
map = { 
	"hair" : "brown",
	"eyes" : "green"
}
try {
	structInsert( map, "hair", "red", false )
} catch (any ex) {
	writeOutput( "error!" )
}

```

Result: error!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjs0KwjAQhM%2FNUyx7qlDoXehBKognhR48r0mgwWQjaWIP4rvbPwv1ODuz3wyxcWQ7qOANIqsvN9gDOu%2BxENn1fBqVN%2FxA8RFlCU3re5ApBM0RaH4Vx%2BSeOVi6a1thvTWxgBeFalGwGyFn7nSIICmC4ehXThNDknF289%2B1AByCAwad9j3OhGnGGuBBGJY2Kd2N1O2gw1Ldm9hOnaSUVv%2B7vty8VYk%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
}
// Show current animals
Dump( label="Current animals", var=animals )
// Insert cat into animals
StructInsert( animals, "cat", "meow" )
// Show animals, now includes cat
Dump( label="Animals with cat added", var=animals )

```

Result: the first dump shows COW and PIG; the second dump shows COW, PIG and CAT

### Overwrite an existing key

With overwrite set to true, an existing key is replaced instead of throwing.

```java
map = { hair : "brown" }
structInsert( map, "hair", "red", true )
writeOutput( map.hair )
```

Result: red
