### Deserialize a JSON string

JSON objects become structs.

```java
json = '{"name":"Luis","age":42}'
result = jsonDeserialize( json )
writeOutput( result.name )
```

Result: Luis

### Deserialize a JSON array

```java
json = '[1, 2, 3]'
result = jsonDeserialize( json )
writeOutput( result.len() )
```

Result: 3

### Key order is preserved

Objects are deserialized into ordered structs, so the keys stay in the order of the JSON string.

```java
result = jsonDeserialize( '{"b":1,"a":2,"c":3}' )
writeOutput( result.keyList() )
```

Result: b,a,c

### Using the member functions

`jsonDeserialize()` and its `fromJSON()` alias are available on strings.

```java
result = '{"id":7}'.fromJSON()
writeOutput( result.id )
```

Result: 7

### Convert a serialized query back to a query

With `strictMapping` set to false, an object with `columns` and `data` keys becomes a query.

```java
json = '{"columns":["id"],"data":[[1],[2]]}'
writeOutput( isQuery( jsonDeserialize( json, false ) ) & "," & isQuery( jsonDeserialize( json ) ) )
```

Result: true,false

### Invalid JSON

A string that is not valid JSON throws an exception.

```java
try {
	jsonDeserialize( "{bad" )
} catch ( any e ) {
	writeOutput( e.message.left( 20 ) )
}
```

Result: Failed to parse JSON
