Use bx:queryParam to protect your application from SQL-injection attacks:

```html
<bx:query name="pages">
    SELECT *
    FROM content
    WHERE id=<bx:queryparam value="#url.id#" />
</bx:query>
```

It is highly recommended to set the `sqltype` of the incoming data:

```html
<bx:query name="pages">
    SELECT *
    FROM content
    WHERE id=<bx:queryparam value="#url.id#" sqltype="integer" />
</bx:query>
```

### Using Lists in QueryParam

For SQL `IN` clauses with comma-separated param values, use `list=true`:

```html
<bx:query name="pages">
    SELECT *
    FROM media
    WHERE type IN <bx:queryparam value="#url.mediaTypes#" list="true" sqltype="varchar" />
</bx:query>
```

Assuming `url.mediaTypes` is equal to `book,magazine,newspaper`, this will generate the following SQL statement:

```sql
SELECT *
FROM books
WHERE title IN (?,?,?)
```

### Basic example

Shows how to use a bx:queryparam tag within bx:query.


```java
<bx:query name="news">
    SELECT id,title,story
    FROM news
    WHERE id = <bx:queryparam value="#url.ID#" sqltype="integer">
</bx:query>
```


### Using a list on an IN statement

Assumes url.idList is a comma separated list of integers, eg: 1,2,3


```java
<bx:query name="news">
    SELECT id,title,story
    FROM news
    WHERE id IN (<bx:queryparam value="#url.IDLIST#" sqltype="integer" list="true">)
</bx:query>
```


### Using an expressions to controll null values

Shows a basic example of using an expression to control whether null is passed to the queryparam


```java
<bx:query name="test">
      INSERT into test ( key, value )
      VALUES(
            <bx:queryparam value="#key#" sqltype="varchar" null="#isNumeric( Key ) == false#">
            <bx:queryparam value="#value#" sqltype="varchar" null="#value == ""#">
      )
</bx:query>
```


### script equivalent of bx:queryparam

Script syntax using queryExecute and struct notation

<a href="https://try.boxlang.io/?code=eJxdj0FLw0AQhc%2FZX%2FHYU4VFqMeKF82KhcZKXRApPQzNqME0qZtZ0yL57262COrtzTDvfW%2F4QLt9zTkJ4Qofgf3xnvsJdFUaqaRmbaJuhF%2FZm0%2Fy2zfycbWGyr5UlsUzjRmmZtSn%2BzjqnHs%2BouQXJungfNhRo1U2mD%2Bui3%2Bughr0VL93aBsUbZssaoOzS%2BW5C7X8NLQH3gbh2PLRLuyNQ8rA7WpZgH899HRnVxZVGX2ziDSI8Hk%2BktWQhvzaPT%2FYkZ1ytRpGWO8r4WWQfZAJTuRzN3cLu8YUqc43E7xZtg%3D%3D" target="_blank">Run Example</a>

```java
exampleData = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
result = queryExecute( "SELECT title FROM exampleData WHERE id = :id", {
	ID : 2
}, {
	DBTYPE : "query"
} );
writeOutput( result.TITLE[ 1 ] );

```

Result: Man walks on Moon

### script equivalent of bx:queryparam

Script syntax using queryExecute and struct notation for multiple parameters

<a href="https://try.boxlang.io/?code=eJx1kFFLwzAUhZ%2BbX3HJ05QibI8TH%2BaS4aDddIuKDBmhvdNi1m5pYjek%2F31pakGHvoSbcE7udw4e5HankEkj4Qb2FvVxhlUPaJaGJjMKaejm3OAb6vBT6uRdave0AhJ8kSBwMgpD6IfN3OrdlTKs8AgpblCaEoS2W5lTEtThL9fgzBXLHCqpPkoocoiLwlvIK1xcE42lVaYj5AdMrEFHueQRHwu4hMliHgP%2BCPN8xxccstR5hu4czRj4Tc29C%2BZYxFRE3C1vsJ5G0SP%2FG6QhHU%2BWD5F4ufeSZLMu92rdNdJmm7Kznwb%2F%2Bb4b9QFrD8JuO4mPSEnd5K50ZnBuzc6aHrQlXHnkFfTBN3MCe1l%2FoA%3D%3D" target="_blank">Run Example</a>

```java
exampleData = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
result = queryExecute( "SELECT * FROM exampleData WHERE id = :id AND title = :title", {
	TITLE : {
		VALUE : "Man walks on Moon",
		sqltype : "varchar"
	},
	ID : {
		VALUE : 2,
		sqltype : "integer"
	}
}, {
	DBTYPE : "query"
} );
writeOutput( result.TITLE[ 1 ] );

```

Result: Man walks on Moon

### script equivalent of bx:queryparam

script syntax using queryExecute and full array notation

<a href="https://try.boxlang.io/?code=eJxdj1FLwzAUhZ%2BbX3HI04QgzEdFRNcMhdbpVhUZMkJ7p8Ws3dLEbsj%2Bu2lKQX27uZwv57u0V5utplhZhUvsHJnDPbUj8LIQtrSauPBzZemdjPhSJv9Qxq%2BWYNE3iyIf4zjHWHRzn%2FdPHlNLBxS0JmUbZMZtVMVZdBR%2FqLN%2FVKoqtEp%2FNqgrpHUdEPaGkwtmqHHaDoZyT7mz5C0XMpGTDOEPTOezFPTroJdbOZcoC89dddJ9%2B%2FN18iSH%2Bsl08Zhkrw%2FdgufrVbPTq%2BHM0C7gmfhmiIR%2Bzo6dVGtKSzNnt86O0BueZndZIpcYI2j%2FAItzZEQ%3D" target="_blank">Run Example</a>

```java
exampleData = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
result = queryExecute( "SELECT title FROM exampleData WHERE id = ?", [
	{
		VALUE : 2,
		sqltype : "varchar"
	}
], {
	DBTYPE : "query"
} );
writeOutput( result.TITLE[ 1 ] );

```

Result: Man walks on Moon

### script equivalent of bx:queryparam

script syntax using queryExecute and array shorthand

<a href="https://try.boxlang.io/?code=eJxdj01Lw0AQhs%2B7v%2BJlTxUWoT0qImhWFBordaFI6WFoRg2mSd3Mmhbpf3eTIlhvM8P78QzvaLOtOCMhXOEzctg%2FcjeCKQsrpVRsbJpr4TcO9ovC%2Bp1COi2h1bdWKskMLjC2%2FXzUp9Vk3PEeBb8ySQsf4oZqo9XBnrgm%2F1w51eio%2BmjR1MibZrDoFc4udeA2VvJL6Ha8jsKJ8tlN3a3HkIG7%2BSwH%2F3loce%2FmDmWRfNc9tFYTvbJICNmNf3lyfekQaPShb%2BlCKTyLso0ywrHy3D%2F4qVtijIHjB54oV6c%3D" target="_blank">Run Example</a>

```java
exampleData = queryNew( "id,title", "integer,varchar", [ 
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Man walks on Moon"
	}
] );
result = queryExecute( "SELECT title FROM exampleData WHERE id = ?", [
	2
], {
	DBTYPE : "query"
} );
writeOutput( result.TITLE[ 1 ] );

```

Result: Man walks on Moon

