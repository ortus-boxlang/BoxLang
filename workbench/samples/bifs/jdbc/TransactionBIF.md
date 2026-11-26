### Begin a database transaction

```java
transaction {
    queryExecute( "CREATE TABLE IF NOT EXISTS test (id INT)" )
}
writeOutput( "transaction complete" )

```

Result: transaction complete

### A transaction block is active only inside its braces

```java
transaction {
	writeOutput( isInTransaction() )
}
writeOutput( " " & isInTransaction() )
```

Result: true false
