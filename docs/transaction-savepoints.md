# Native transaction savepoints

Use `transactionSetSavepoint(name)`, `transactionRollback(name)`, and
`transactionReleaseSavepoint(name)` inside a native transaction. Release removes
rollback markers without committing or rolling back the database changes.

```boxlang
transaction {
    transactionSetSavepoint("optionalWork")
    try {
        performOptionalWork()
    } catch (any error) {
        transactionRollback("optionalWork")
        recordOptionalFailure(error)
    }
    transactionReleaseSavepoint("optionalWork")
    performRequiredWork()
}
```

The component equivalent is:

```boxlang
transaction action="releasesavepoint" savepoint="optionalWork";
```

Names are case insensitive and must be nonblank. A name cannot be reused while
its savepoint is active; it can be reused after release. Releasing an unknown or
already released savepoint throws a `DatabaseException`. The function requires an
active transaction. The component retains the other transaction actions' contract
of doing nothing outside an active transaction.

Rollback retains its target marker and invalidates markers created after it.
Release invalidates its target and all later markers. A full commit, full rollback,
or transaction end clears all markers. This conservative lifecycle matches
PostgreSQL savepoint nesting and prevents later use of invalid JDBC handles.

A savepoint requested before the first query is recorded immediately and created
on the transactional connection before that query executes. This preserves lazy
datasource selection and connection acquisition. Releasing or rolling back pending
savepoints does not require opening a connection. The Java `setSavepoint()` method
returns `null` for a pending savepoint; its registry entry is populated when the
connection is acquired.

When nested transactions are enabled, child savepoint names are isolated from
parent names. Child release operates within the child's namespace, preserves the
parent rollback boundary, and synchronizes invalidated handles with the parent
registry. Internal child BEGIN, COMMIT, and END markers cannot be explicitly
released. Ordinary child completion continues to leave internal markers for the
outer transaction to clear, preserving compatibility with drivers that do not
support savepoint release.

Release delegates to JDBC `Connection.releaseSavepoint()`. Drivers that do not
support it raise a `DatabaseException`; BoxLang does not silently skip the operation
or fall back to SQL. A failed JDBC release or rollback leaves the registry unchanged
so the caller can recover. `onTransactionReleaseSavepoint` is announced through the
runtime and application interception pools before release, using the same
transaction, connection, context, and savepoint fields as the existing savepoint
event. A pending release has a null connection.
