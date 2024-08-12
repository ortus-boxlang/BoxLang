#### Using TransactionSetSavepoint with TransactionRollback

Here's a simple example of using multiple savepoints within a transaction:

```java
transaction {
    queryExecute( "INSERT INTO vehicles (id,make) VALUES (8, 'Ford' )", {}, { datasource : "carDB" } );
    transactionSetSavepoint( 'insert' );

    queryExecute( "UPDATE developers SET name = 'Chevrolet' WHERE id=8", {}, { datasource : "carDB" } );
    transactionSetSavepoint( 'update' );

    // more stuff ...
    transactionRollback( 'insert' );
}
```

In this example, the UPDATE will be rolled back while the INSERT statement will remain. Remember that in `transactionRollback()`, the savepoint is not the name of a single savepoint to roll back, but the name of the savepoint to rollback TO.
