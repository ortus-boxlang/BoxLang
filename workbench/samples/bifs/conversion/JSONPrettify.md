### Prettify a JSON string

Newlines are shown here as `~`.

```java
json = '{"name":"Luis","age":42}'
result = jsonPrettify( json )
writeOutput( result.replace( char( 10 ), "~", "all" ) )
```

Result: {~  "name" : "Luis",~  "age" : 42~}

### Using the member function

```java
pretty = '{"a":1}'.jsonPrettify()
writeOutput( pretty.listToArray( char( 10 ) ).len() )
```

Result: 3

### Prettify nested JSON

Arrays are kept on a single line.

```java
result = jsonPrettify( '{"list":[1,2]}' )
writeOutput( result.replace( char( 10 ), "~", "all" ) )
```

Result: {~  "list" : [ 1, 2 ]~}
