### Serialize a struct to JSON

An ordered struct (square bracket syntax) keeps its key order.

```java
data = [ name: "Luis", age: 42 ]
writeOutput( jsonSerialize( data ) )
```

Result: {"name":"Luis","age":42}

### Serialize an array to JSON

```java
items = [ 1, 2, 3 ]
writeOutput( jsonSerialize( items ) )
```

Result: [1,2,3]

### Pretty print the JSON

Set `pretty` to true for indentation and line breaks. Newlines are shown here as `~`.

```java
data = [ name: "Luis", age: 42 ]
result = jsonSerialize( data = data, pretty = true )
writeOutput( result.replace( char( 10 ), "~", "all" ) )
```

Result: {~  "name" : "Luis",~  "age" : 42~}

### Using the toJSON member function

`toJSON()` is available on arrays, structs, queries, sets, strings, numbers and booleans.

```java
writeOutput( [ 1, 2 ].toJSON() & " " & "text".toJSON() & " " & true.toJSON() )
```

Result: [1,2] "text" true

### Serialize a list with listToJSON

The string is split as a comma-delimited list and serialized as a JSON array.

```java
writeOutput( "foo,bar,baz".listToJSON() )
```

Result: ["foo","bar","baz"]

### Serialize a query as an array of structs

The `struct` query format creates an array with one struct per row.

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "a" ], [ 2, "b" ] ] )
writeOutput( jsonSerialize( q, "struct" ) )
```

Result: [{"id":1,"name":"a"},{"id":2,"name":"b"}]

### Serialize a query by column

The `column` format creates a struct with the row count, the columns, and the data of each column.

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "a" ], [ 2, "b" ] ] )
writeOutput( jsonSerialize( q, "column" ) )
```

Result: {"rowCount":2,"columns":["id","name"],"data":{"id":[1,2],"name":["a","b"]}}

### Serialize a query by row

The `row` format creates a struct with the columns and an array of row arrays.

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "a" ], [ 2, "b" ] ] )
writeOutput( jsonSerialize( q, "row" ) )
```

Result: {"columns":["id","name"],"data":[[1,"a"],[2,"b"]]}

### Invalid query format

Only true, false, row, column and struct are accepted.

```java
try {
	jsonSerialize( queryNew( "id" ), "weird" )
} catch ( any e ) {
	writeOutput( "invalid format" )
}
```

Result: invalid format
