### Delete a registered executor by name

```java
executorNew( "tempPool", "fixed", 4 );
executorDelete( "tempPool" );
writeOutput( executorHas( "tempPool" ) );

```

Result: false

### Delete returns the deleted executor record

```java
executorNew( "myPool", "cached" );
deleted = executorDelete( "myPool" );
writeOutput( isNull( deleted ) ? "null" : "deleted" );

```

Result: deleted
