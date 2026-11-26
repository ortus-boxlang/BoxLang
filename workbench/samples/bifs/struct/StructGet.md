### Get a value using structGet

Resolves a variable path from a string.

```java
x = { y : { z : 8 } }
writeOutput( structGet( "x.y.z" ) )
```

Result: 8

### Get a value using the member function

As a member function (getFromPath), the path is relative to the struct.

```java
x = { y : { z : 8 } }
writeOutput( x.getFromPath( "y.z" ) )
```

Result: 8

### Missing paths return null and do not modify the struct

```java
x = { y : { z : 8 } }
value = structGet( "x.a.b" )
writeOutput( isNull( value ) & " " & structKeyExists( x, "a" ) )
```

Result: true false
