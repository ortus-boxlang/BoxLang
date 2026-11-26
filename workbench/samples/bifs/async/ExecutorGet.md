### Get a registered executor by name

```java
executorNew( "myPool", "fixed", 4 )
exec = executorGet( "myPool" )
writeOutput( exec.name() )
```

Result: myPool

### Get the default executor

When no name is passed, the default `io-tasks` executor is returned.

```java
writeOutput( executorGet().name() )
```

Result: io-tasks

### Get the type of an executor

```java
executorNew( "workPool", "cached" )
exec = executorGet( "workPool" )
writeOutput( exec.type().toString() )
```

Result: CACHED

### Executor not found

An exception is thrown if the executor is not registered.

```java
try {
	executorGet( "nope" )
} catch ( any e ) {
	writeOutput( e.message.left( 30 ) )
}
```

Result: Executor [nope] does not exist
