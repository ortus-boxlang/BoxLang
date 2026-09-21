### Get the keys of a struct as a Set

Returns a BoxSet containing all keys of the struct.

```java
s = { name: "Luis", age: 42, email: "x@y.z" }.keySet();
writeOutput( s.size() );

```

Result: 3

### Using the standalone function form

```java
s = structKeySet( { a: 1, b: 2, c: 3 } );
writeOutput( s.size() );

```

Result: 3

### Case sensitivity mirrors the source struct

A case-sensitive struct produces a case-sensitive key Set.

```java
cs = structNew( "casesensitive" );
cs[ "Name" ] = "Luis";
cs[ "name" ] = "Brad";
keys = cs.keySet();
writeOutput( keys.isCaseSensitive() & "," & keys.size() );

```

Result: true,2
