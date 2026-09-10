### Shutdown a registered executor

```java
executorNew( "tempPool", "fixed", 2 );
executorShutdown( "tempPool" );
writeOutput( executorHas( "tempPool" ) );

```

Result: false

### Shutdown with timeout

```java
executorNew( "workPool", "fixed", 4 );
executorShutdown( "workPool", 5000 );
writeOutput( executorHas( "workPool" ) );

```

Result: false
