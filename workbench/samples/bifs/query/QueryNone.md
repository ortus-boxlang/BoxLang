### Test that no query rows satisfy a predicate

Returns `true` when the callback returns `false` for every row.

```java
q = queryNew( "name,age", "varchar,integer", [
    [ "Alice", 25 ],
    [ "Bob", 30 ]
] );
writeOutput( q.none( ( row ) => row.age > 100 ) );

```

Result: true

### Returns false when at least one row matches

```java
q = queryNew( "name,score", "varchar,integer", [
    [ "Alice", 85 ],
    [ "Bob", 45 ]
] );
writeOutput( q.none( ( row ) => row.score > 80 ) );

```

Result: false
