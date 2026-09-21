### Return the first future to complete

Executes all futures in parallel and returns the result of the fastest one.

```java
slow = asyncRun( () => sleep( 200 ) && return "slow" );
fast = asyncRun( () => sleep( 50 ) && return "fast" );
result = anyOf( [ slow, fast ] ).get();
writeOutput( result );

```

Result: fast

### Race multiple computations

```java
results = anyOf( [
    () => sleep( 100 ) && return "A",
    () => sleep( 10 ) && return "B",
    () => sleep( 50 ) && return "C"
] ).get();
writeOutput( results );

```

Result: B
