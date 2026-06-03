### Deserialize a JSON string

```java
json = '{"name":"Luis","age":42}';
result = jsonDeserialize( json );
writeOutput( result.name );

```

Result: Luis

### Deserialize a JSON array

```java
json = '[1, 2, 3]';
result = jsonDeserialize( json );
writeOutput( result.len() );

```

Result: 3
