### Check if an executor exists by name

```java
executorNew( "myPool", "fixed", 4 );
writeOutput( executorHas( "myPool" ) );

```

Result: true

### Returns false for non-existent executor

```java
writeOutput( executorHas( "nonexistent" ) );

```

Result: false
