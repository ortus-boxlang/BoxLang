### Create a new executor service

```java
exec = executorNew( "myPool", "fixed", 4 );
writeOutput( exec.name() & "," & exec.type() );

```

Result: myPool,FIXED

### Available executor types

```java
executorNew( "cachedPool", "cached" );
executorNew( "singleThread", "single" );
executorNew( "virtualThreads", "virtual" );
writeOutput( executorHas( "cachedPool" ) & executorHas( "singleThread" ) & executorHas( "virtualThreads" ) );

```

Result: truetruetrue

### Cached thread pool (unbounded)

```java
exec = executorNew( "workPool", "cached" );
writeOutput( exec.type() );

```

Result: CACHED
