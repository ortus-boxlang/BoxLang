### Create a new future for async execution

```java
future = futureNew( () => 42 )
writeOutput( future.get() )

```

Result: 42

### Create a completed future with a value

```java
future = futureNew( "already done" )
writeOutput( future.isDone() & "," & future.get() )

```

Result: true,already done

### Create an incomplete future and complete it later

```java
future = futureNew()
future.complete( "later" )
writeOutput( future.get() )

```

Result: later

### Wrap a value that is not a function

Any value that is not a function or a Java CompletableFuture produces an already completed future.

```java
future = futureNew( [ 1, 2, 3 ] )
writeOutput( future.get().len() )
```

Result: 3
