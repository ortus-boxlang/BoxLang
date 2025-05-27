### Using TransactionCommit with TransactionRollback

This simple example shows two JDBC queries executed inside a transaction. The first query, an INSERT, is committed to the database while the second is rolled back.

```java
transaction {
    queryExecute( "INSERT INTO vehicles (id,make) VALUES (8, 'Ford' )", {}, { datasource : "carDB" } );
    transactionCommit();

    queryExecute( "UPDATE developers SET name = 'Chevrolet' WHERE id=8", {}, { datasource : "carDB" } );
    transactionRollback();
}
```

Note that the rollback cannot affect the INSERT statement, since it has already been committed ("persisted") to the database.

### Script Syntax

In this code, transactionCommit() will commit the pending transaction, which is any database queries run in the transaction block.

<a href="https://try.boxlang.io/?code=eJxLqrAqKUrMK05MLsnMz1Oo5uIsKaoEUZz6%2BgrJ%2BSmpCiX5CkWleUCBJFSlEMpWKTk%2FNzezRMmai7NWITmxJDlDQSMxr1IhVRNsCg5NRfk5OUmJydlgbVy1XADL1Cvw" target="_blank">Run Example</a>

```java
bx:transaction {
	try {
		// code to run
		bx:transaction action="commit";
	} catch (any e) {
		bx:transaction action="rollback";
	}
}

```

Result: 

