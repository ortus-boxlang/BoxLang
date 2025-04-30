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