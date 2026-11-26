### Return the first future to complete

Executes all futures in parallel and returns the result of the first one to finish.

```java
slow = asyncRun( () => {
	sleep( 300 )
	return "slow"
} )
fast = asyncRun( () -> "fast" )
result = asyncAny( [ slow, fast ] ).get()
writeOutput( result )
```

Result: fast

### Race multiple functions

Functions in the array are executed in parallel and the fastest result wins.

```java
result = asyncAny( [
	() => {
		sleep( 300 )
		return "A"
	},
	() -> "B"
] ).get()
writeOutput( result )
```

Result: B

### Chain a function on the winner

```java
message = asyncAny( [ () -> 5 ] )
	.then( ( n ) -> "winner: " & n )
	.get()
writeOutput( message )
```

Result: winner: 5
