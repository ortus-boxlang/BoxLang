### Convert to an unmodifiable copy

```java
data = { name: "test" }
frozen = toUnmodifiable( data )
writeOutput( frozen.name )
```

Result: test

### Mutating an unmodifiable array throws an error

```java
data = [ 1, 2, 3 ]
frozen = data.toUnmodifiable()
try {
	frozen.append( 4 )
	writeOutput( "no error" )
} catch ( any e ) {
	writeOutput( "caught" )
}
```

Result: caught

### Mutating an unmodifiable struct throws an error

```java
frozen = toUnmodifiable( { a: 1 } )
try {
	frozen.b = 2
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Cannot modify Unmodifiable Struct

### The original value is not affected

The unmodifiable value is a copy, so later changes to the original are not visible in it.

```java
original = [ 1, 2, 3 ]
frozen = original.toUnmodifiable()
original.append( 4 )
writeOutput( original.len() & "," & frozen.len() )
```

Result: 4,3

### Only arrays, structs, queries and sets can be converted

```java
try {
	toUnmodifiable( "text" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Cannot convert value to Unmodifiable type as it is not a struct, array, query or set
