### Convert to a modifiable struct

```java
data = { name: "test" }
frozen = toUnmodifiable( data )
modifiable = toModifiable( frozen )
modifiable.name = "changed"
writeOutput( modifiable.name )
```

Result: changed

### Convert an array using the member function

An unmodifiable array cannot be appended to, but its modifiable version can.

```java
frozen = [ 1, 2, 3 ].toUnmodifiable()
list = frozen.toModifiable()
list.append( 4 )
writeOutput( list.len() )
```

Result: 4

### Values that are not unmodifiable are returned as is

```java
writeOutput( toModifiable( "hello" ) )
```

Result: hello
