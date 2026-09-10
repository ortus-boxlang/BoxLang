### Create an Attempt with a value

Wraps a value in an Attempt object for fluent error-safe operations.

```java
attempt = attempt( 42 );
writeOutput( attempt.get() );

```

Result: 42

### Create an Attempt with a closure

The closure is executed and its result (or exception) is captured.

```java
attempt = attempt( () => 10 / 2 );
writeOutput( attempt.get() );

```

Result: 5

### Attempt that catches an exception

When the closure throws, the Attempt captures the failure instead of crashing.

```java
attempt = attempt( () => 10 / 0 );
writeOutput( attempt.hasError() );

```

Result: true

### Fluent chaining with Attempt

```java
result = attempt( () => "hello".len() )
    .map( (x) => x * 2 )
    .getOrDefault( 0 );
writeOutput( result );

```

Result: 10

### Attempt with no value

Creates an empty Attempt object for later use.

```java
attempt = attempt();
writeOutput( attempt.isEmpty() );

```

Result: true
