### Convert a query string to a struct

```java
result = queryStringToStruct( "name=Luis&age=42&city=NYC" )
writeOutput( result.name )

```

Result: Luis

### URL decoding, leading question mark and empty values

```java
s = queryStringToStruct( "?name=Luis%20Majano&flag" )
writeOutput( s.name & "|" & s.flag & "|" )

```

Result: Luis Majano||

### Custom delimiter

```java
s = queryStringToStruct( "a=1;b=2", ";" )
writeOutput( s.a & s.b )

```

Result: 12
