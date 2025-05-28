### Check JDBC Version

```js
bx:dbinfo( type='version', name='result' );
```

The `result` variable is now populated with a query looking something like this:

| DATABASE_PRODUCTNAME | DATABASE_VERSION | DRIVER_NAME        | DRIVER_VERSION | JDBC_MAJOR_VERSION | JDBC_MINOR_VERSION |
|-----------------------|------------------|--------------------|----------------|--------------------|--------------------|
| MySQL                | 8.0.33          | MySQL Connector/J  | 8.0.33        | 4                  | 2                  |

### Read All Database Views

Use `type=tables` to read all database tables, or filter for a specific type of table using `filter=TABLE|VIEW|SYSTEM TABLE|GLOBAL TEMPORARY|LOCAL TEMPORARY`:

```js
bx:dbinfo( type='tables', name='result', filter='VIEW' );
```

| TABLE_CAT | TABLE_SCHEM | TABLE_NAME | TABLE_TYPE | REMARKS       | TYPE_CAT | TYPE_SCHEM | TYPE_NAME | SELF_REFERENCING_COL_NAME | REF_GENERATION |
|-----------|-------------|------------|------------|---------------|----------|------------|-----------|---------------------------|----------------|
| mydb      | public      | my_view    | VIEW       | Example view  | NULL     | NULL       | NULL      | NULL                      | NULL           |

### Read Table Columns

```js
bx:dbinfo( type='columns', name='result', table='admins' );
```

The `admins` table column information is now populated into the `result` variable. The result columns are:

* TABLE_CAT
* TABLE_SCHEM
* TABLE_NAME
* COLUMN_NAME
* DATA_TYPE
* TYPE_NAME
* COLUMN_SIZE
* BUFFER_LENGTH
* DECIMAL_DIGITS
* NUM_PREC_RADIX
* NULLABLE
* REMARKS
* COLUMN_DEF
* SQL_DATA_TYPE
* SQL_DATETIME_SUB
* CHAR_OCTET_LENGTH
* ORDINAL_POSITION
* IS_NULLABLE
* SCOPE_CATALOG
* SCOPE_SCHEMA
* SCOPE_TABLE
* SOURCE_DATA_TYPE
* IS_AUTOINCREMENT
* IS_GENERATEDCOLUMN
* SCOPE_CATLOG
* IS_PRIMARYKEY
* IS_FOREIGNKEY
* REFERENCED_PRIMARYKEY
* REFERENCED_PRIMARYKEY_TABLE
### Output Column Names

Along with the data type and size


```java
<bx:dbinfo type="columns" name="cols" table="tester">
<bx:output query="cols">
        #cols.COLUMN_NAME# #cols.TYPE_NAME#(#cols.COLUMN_SIZE#)<br>
</bx:output>

```


