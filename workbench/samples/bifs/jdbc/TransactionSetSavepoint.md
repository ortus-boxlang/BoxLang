#### Using TransactionSetSavepoint with TransactionRollback

Here's a simple example of using multiple savepoints within a transaction:

```java
transaction {
    queryExecute( "INSERT INTO vehicles (id,make) VALUES (8, 'Ford' )", {}, { datasource : "carDB" } )
    transactionSetSavepoint( 'insert' )

    queryExecute( "UPDATE developers SET name = 'Chevrolet' WHERE id=8", {}, { datasource : "carDB" } )
    transactionSetSavepoint( 'update' )

    // more stuff ...
    transactionRollback( 'insert' )
}
```

In this example, the UPDATE will be rolled back while the INSERT statement will remain. Remember that in `transactionRollback()`, the savepoint is not the name of a single savepoint to roll back, but the name of the savepoint to rollback TO.

### Roll back to a specified savepoint

This example runs multiple queries with a savepoint for each, and rolls back to the 'useradded' savepoint if the last query fails.

<a href="https://try.boxlang.io/?code=eJxNjDEOgzAQBGv7FSsqu6LPM8ILDvskrKBzYo4oVsTfyaWi2WZmdv7ctJFslLRUwde7cUSRjZti%2F613FzyxTvTmZy2iAYNxypnzgGhet%2Fz4P5BUXbjhtXPr3iGRpgWBpIOjadfbe13XmdIjRMuPE2qqMFc%3D" target="_blank">Run Example</a>

```java
bx:transaction {
	// insert user
	transactionSetSavepoint( "useradded" )
	try {
	}
	// another query
	 catch (any e) {
		transactionRollback()
	}
}

```

### Setting a savepoint outside of a transaction throws an error

```java
try {
	transactionSetSavepoint( "beforeUpdate" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Transaction not started; Please place this method call inside a transaction{} block.

### Set a savepoint and roll back to it

```java
transaction {
	transactionSetSavepoint( "beforeUpdate" )
	transactionRollback( "beforeUpdate" )
	writeOutput( "rolled back to beforeUpdate" )
}
```

Result: rolled back to beforeUpdate
