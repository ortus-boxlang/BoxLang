### Wait for all futures to complete

Accepts an array of futures/closures/lambdas and executes them all in parallel. The results are returned in an array once every future has completed.

```java
f1 = asyncRun( () -> "one" )
f2 = asyncRun( () -> "two" )
f3 = asyncRun( () -> "three" )
results = asyncAll( [ f1, f2, f3 ] ).get()
writeOutput( results.toList() )
```

Result: one,two,three

### Results are returned in the order they were passed

Even though the first future finishes last, its result is still first. The closures call `sleep()`, so they use `=>`.

```java
slow = asyncRun( () => {
	sleep( 200 )
	return "slow"
} )
fast = asyncRun( () => "fast" )
results = asyncAll( [ slow, fast ] ).get()
writeOutput( results.toList() )
```

Result: slow,fast

### Using lambdas directly without pre-creating futures

Functions in the array are treated as futures and executed in parallel.

```java
results = asyncAll( [
	() -> 10 * 2,
	() -> 20 * 2,
	() -> 30 * 2
] ).get()
writeOutput( results.toList() )
```

Result: 20,40,60

### Chain a function on the combined future

The returned BoxFuture can be chained, so you can process the array of results once all futures complete.

```java
total = asyncAll( [ () -> 1, () -> 2, () -> 3 ] )
	.then( ( values ) -> values.sum() )
	.get()
writeOutput( total )
```

Result: 6

### Invalid executor name

An exception is thrown if the executor name is not registered.

```java
try {
	asyncAll( [ () -> 1 ], "doesNotExist" )
} catch ( any e ) {
	writeOutput( e.message.left( 30 ) )
}
```

Result: Executor [doesNotExist] does not
