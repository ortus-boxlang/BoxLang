### Prettify a JSON string

```java
json = '{"name":"Luis","age":42}';
result = jsonPrettify( json );
writeOutput( result.contains( chr(10) ) );

```

Result: true

### Custom indentation

```java
json = '{"a":1,"b":2}';
result = jsonPrettify( json, "  " );
writeOutput( result.contains( "  " ) );

```

Result: true
