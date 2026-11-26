### Release a savepoint once it is no longer needed

Assumes a datasource whose JDBC driver supports releasing savepoints. Releasing a savepoint does not commit or roll back anything; it just means the transaction can no longer be rolled back to that savepoint.

```java
transaction {
	queryExecute( "INSERT INTO vehicles ( id, make ) VALUES ( 8, 'Ford' )", {}, { datasource: "carDB" } )
	transactionSetSavepoint( "afterInsert" )
	queryExecute( "UPDATE vehicles SET make = 'Chevrolet' WHERE id = 8", {}, { datasource: "carDB" } )
	transactionReleaseSavepoint( "afterInsert" )
	transactionCommit()
}
writeOutput( "done" )
```

Result: done

### Releasing a savepoint outside of a transaction throws an error

```java
try {
	transactionReleaseSavepoint( "afterInsert" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Transaction not started; Please place this method call inside a transaction{} block.
