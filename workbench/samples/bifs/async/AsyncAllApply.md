### Apply a function to each item in parallel

Applies the function to every item of an array in parallel and returns the array of results, in order. This BIF blocks until all the items are done.

```java
squares = asyncAllApply( [ 1, 2, 3, 4, 5 ], ( n ) -> n * n )
writeOutput( squares.toList() )
```

Result: 1,4,9,16,25

### Process a struct of items

When the items are a struct, the mapper receives each entry as an item with `key` and `value`, and the result is a struct.

```java
doubled = asyncAllApply( { a: 10, b: 20 }, ( item ) -> item.value * 2 )
writeOutput( doubled.a & "," & doubled.b )
```

Result: 20,40

### Handle errors with an error handler

If the mapper throws, the error handler receives the exception and its return value is used for that item.

```java
results = asyncAllApply(
	[ 1, 2, 0 ],
	( n ) -> 10 / n,
	( e ) -> "error"
)
writeOutput( results.toList() )
```

Result: 10,5,error

### Using a named executor

The executor can be the name of a registered executor, such as the default `io-tasks`.

```java
results = asyncAllApply(
	items = [ 1, 2, 3 ],
	mapper = ( n ) -> n + 1,
	executor = "io-tasks"
)
writeOutput( results.toList() )
```

Result: 2,3,4
