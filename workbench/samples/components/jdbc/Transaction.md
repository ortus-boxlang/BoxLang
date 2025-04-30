### Simple Transaction

A "simple" transaction might look like this:

```js
transaction{
    queryExecute( "UPDATE users SET name=:name WHERE id=:id", { id: variables.id, name: variables.name } );
    queryExecute(
        "INSERT INTO userLog( action, id, changes ) VALUES ( 'UPDATE', :id, :change)",
        { id: variables.id, change: "changed name to #variables.name#" }
    );
}
```

Note that you can specify a custom isolation level using the `isolation` attribute on the component:

```js
transaction isolation="serializable"{
    // ...
}
```


### Nested Transaction

BoxLang supports nested transactions, using savepoints to control transaction state between the parent and child transactions.

```js
transaction{
    queryExecute( "UPDATE users SET name=:name WHERE id=:id", { id: variables.id, name: variables.name } );

    transaction{
        try{
            queryExecute(
                "INSERT INTO userLog( action, id, changes ) VALUES ( 'UPDATE', :id, :change)",
                { id: variables.id, change: "changed name to #variables.name#" }
            );
        } catch( any e ){
            // user log errored; rollback!
            transactionRollback();
            // log error here
        }
    }
}
```

Here, we have a nested transaction that may (or may not) error; if this happens we want to roll back the logging query but NOT roll back the user query. This functions the same as if we were rolling back to a named savepoint created just after the `UPDATE users` query.