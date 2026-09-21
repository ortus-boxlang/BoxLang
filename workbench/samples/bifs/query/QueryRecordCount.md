### Get the number of rows in a query

```java
q = queryNew( "name", "varchar", [ [ "Alice" ], [ "Bob" ], [ "Carol" ] ] );
writeOutput( q.recordCount() );

```

Result: 3

### Using the global function form

```java
q = queryNew( "id,value", "integer,varchar", [ [ 1, "a" ], [ 2, "b" ] ] );
writeOutput( queryRecordCount( q ) );

```

Result: 2
