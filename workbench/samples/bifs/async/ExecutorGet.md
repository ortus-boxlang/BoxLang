### Get a registered executor by name

```java
executorNew( "myPool", "fixed", 4 );
exec = executorGet( "myPool" );
writeOutput( exec.name() );

```

Result: myPool

### Get executor details

```java
executorNew( "workPool", "cached" );
exec = executorGet( "workPool" );
writeOutput( exec.type() );

```

Result: CACHED
