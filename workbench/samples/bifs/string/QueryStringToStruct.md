### Convert a query string to a struct

```java
result = queryStringToStruct( "name=Luis&age=42&city=NYC" );
writeOutput( result.name );

```

Result: Luis

### Using the member function

```java
result = "foo=bar&baz=qux".queryStringToStruct();
writeOutput( result.foo & "," & result.baz );

```

Result: bar,qux
