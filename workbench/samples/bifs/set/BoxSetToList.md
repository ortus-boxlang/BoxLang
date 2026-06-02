### Convert a Set to a comma-delimited list

```java
s = setNew( type="linked", values=[ "a", "b", "c" ] );
writeOutput( s.toList() );

```

Result: a,b,c

### Using a custom delimiter

```java
s = setNew( type="linked", values=[ "a", "b", "c" ] );
writeOutput( s.toList( "-" ) );

```

Result: a-b-c
