### Preserve single quotes in a SQL string

Prevents double-escaping of single quotes in dynamic SQL.

```java
sql = "SELECT * FROM users WHERE name = '#preserveSingleQuotes( "O'Brien" )#'";
writeOutput( sql.contains( "O'Brien" ) );

```

Result: true
