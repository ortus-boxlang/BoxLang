### Implicit deletion of the last row

Builds a simple query and removes the last row by not specifying a row index.


```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
queryDeleteRow( news );
writeOutput( news[ "title" ][ 1 ] );

```

Result: Dewey defeats Truman

### Deletes a specific row from the query

Builds a simple query and removes one of the rows.

<a href="https://try.boxlang.io/?code=eJxdjjELwjAUhOfkVxyZFLLUUXHrWgviVjoE%2B9RiTTR9MRTxv5u2OOh27%2B6%2Bx1mKPbZ4BPLDjuICqm00t9yR0klbpjN5%2FTT%2BeDE%2BWRWkeEkhUk1hjUyPeu6nU%2BUUaUBDJzLc4%2BDDzVglxVv%2FUKs%2FqjAW0XTXHs6icG5CZI3lRk7DcuqIae%2FSPJv2amRjFH3LVAa%2BB579Ct%2BfdZUqE%2F8BgdlAzA%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
queryDeleteRow( news, 1 );
writeOutput( news[ "title" ][ 1 ] );

```

Result: Man walks on Moon

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kLEKwjAQhufkKY5MKcRC01F0ad2khaKTOMQQsWCtjamliO%2FutcZBcXFw%2BiDff%2BG%2Fa2wfwQya1tg%2BMx0HpsROaCaAXZXVB2XFB1FtgJINJYSpiImBO08dMUq24mWlt55avtnYW08dD5ZuIZjSsU5qjsaZosZSDbYUIAfV2dKZtK3O%2FFk6qY9tdUqVU68YUwyC0NXL8uJ4MH5ne%2Fn7kn%2FecV9bDiXWiqeI%2BQwi5GQCAdwo%2BXIAKTCGc%2Fe3E%2BB7WCySvEiTfJ2thsAD54lxZQ%3D%3D" target="_blank">Run Example</a>

```java
qry1 = queryNew( "a,b,c", "varchar,varchar,varchar", [ 
	[
		"a1",
		"b1",
		"c1"
	],
	[
		"a2",
		"b2",
		"c2"
	],
	[
		"a3",
		"b3",
		"c3"
	]
] );
queryDeleteRow( qry1, 2 );
writeDump( queryColumnData( qry1, "a" ).toList() );
qry2 = queryNew( "a,b,c", "varchar,varchar,varchar", [
	[
		"a1",
		"b1",
		"c1"
	],
	[
		"a2",
		"b2",
		"c2"
	],
	[
		"a3",
		"b3",
		"c3"
	]
] );
for( i = 3; i >= 1; i-- ) {
	queryDeleteRow( qry2, i );
}
writeDump( qry2.RECORDCOUNT );

```


