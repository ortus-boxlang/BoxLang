### Apply a function to each item in parallel

Applies the function to every item of an array in parallel and returns the array of results, in order. This BIF blocks until all the items are done.

```java
squares = asyncAllApply( [ 1, 2, 3, 4, 5 ], ( n ) -> n * n )
writeOutput( squares.toList() )
```

Result: 1,4,9,16,25

### Process a struct of items

When the items are a struct, the mapper receives each entry as a struct with `key` and `value`, and must return a struct with a `value` key. The result is a struct with the original keys.

```java
doubled = asyncAllApply( { a: 10, b: 20 }, ( item ) -> { value: item.value * 2 } )
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

### Timeout

If the items do not all complete within the timeout, an exception is thrown. Here named arguments are used to pass only the timeout and its time unit.

```java
try {
	asyncAllApply(
		items = [ 1 ],
		mapper = ( n ) => {
			sleep( 1000 )
		},
		timeout = 100,
		timeUnit = "milliseconds"
	)
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Array processing timed out after 100 MILLISECONDS
