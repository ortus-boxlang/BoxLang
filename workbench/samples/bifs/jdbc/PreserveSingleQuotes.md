### Preserve single quotes in a SQL string

Prevents double-escaping of single quotes in dynamic SQL.

```java
sql = "SELECT * FROM users WHERE name = '#preserveSingleQuotes( "O'Brien" )#'"
writeOutput( sql.contains( "O'Brien" ) )

```

Result: true

### The value is returned unchanged

```java
writeOutput( preserveSingleQuotes( "O'Brien" ) )
```

Result: O'Brien

### Preserve the quotes of a prepared list of values inside a query

```java
users = queryNew( "id,name", "varchar,varchar", [ [ "1", "Han" ], [ "2", "Leia" ], [ "3", "Luke" ] ] )
names = "'Han','Luke'"
result = queryExecute(
	"SELECT name FROM users WHERE name IN (#preserveSingleQuotes( names )#) ORDER BY id",
	[],
	{ dbtype: "query" }
)
writeOutput( queryColumnData( result, "name" ).toList() )
```

Result: Han,Luke
