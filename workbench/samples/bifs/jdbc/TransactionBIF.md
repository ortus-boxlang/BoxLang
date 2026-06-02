### Begin a database transaction

```java
transaction {
    queryExecute( "CREATE TABLE IF NOT EXISTS test (id INT)" );
}
writeOutput( "transaction complete" );

```

Result: transaction complete
