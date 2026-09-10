### Prettify and format SQL queries

```java
sql = "SELECT id, name FROM users WHERE active = 1 ORDER BY name";
result = sqlPrettify( sql );
writeOutput( result.contains( "SELECT" ) );

```

Result: true

### Complex query formatting

```java
sql = "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id WHERE o.total > 100";
result = sqlPrettify( sql );
writeOutput( isString( result ) );

```

Result: true

### Using the member function

```java
result = "SELECT * FROM users".sqlPrettify();
writeOutput( result.len() > 20 );

```

Result: true
