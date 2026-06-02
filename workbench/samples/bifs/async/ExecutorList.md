### List all registered executors

```java
executorNew( "pool1", "fixed", 2 );
executorNew( "pool2", "cached" );
execs = executorList();
writeOutput( execs.len() gte 2 );

```

Result: true

### Executor list contains executor names

```java
executorNew( "myExecutor", "single" );
execs = executorList();
writeOutput( execs.some( ( e ) => e.name() == "myExecutor" ) );

```

Result: true
