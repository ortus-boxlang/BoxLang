### Wait for all async operations to complete

Accepts an array of futures/closures/lambdas and executes them all in parallel.

```java
f1 = asyncRun( () => sleep( 100 ) && return "one" );
f2 = asyncRun( () => sleep( 100 ) && return "two" );
f3 = asyncRun( () => sleep( 100 ) && return "three" );
results = all( [ f1, f2, f3 ] ).get();
writeOutput( results.len() );

```

Result: 3

### Results are returned in the order they were passed

```java
slow = asyncRun( () => sleep( 200 ) && return "slow" );
fast = asyncRun( () => sleep( 50 ) && return "fast" );
results = all( [ slow, fast ] ).get();
writeOutput( results[ 1 ] );

```

Result: slow

### Using closures directly without pre-creating futures

```java
results = all( [
    () => 10 * 2,
    () => 20 * 2,
    () => 30 * 2
] ).get();
writeOutput( results.toString() );

```

Result: [20, 40, 60]
