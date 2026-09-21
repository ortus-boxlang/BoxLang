### Test whether a Set contains a value

```java
s = [ 1, 2, 3 ].toSet();
writeOutput( s.contains( 2 ) & "," & s.contains( 99 ) );

```

Result: true,false

### Using the has alias

`has` is an alias for `contains`.

```java
s = setOf( "apple", "banana" );
writeOutput( s.has( "banana" ) & "," & s.has( "mango" ) );

```

Result: true,false

### Case-insensitive lookup (default)

```java
s = setNew( values=[ "Foo", "Bar" ] );
writeOutput( s.contains( "foo" ) );

```

Result: true
