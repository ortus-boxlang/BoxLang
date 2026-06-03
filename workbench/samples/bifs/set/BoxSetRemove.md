### Remove a single element

If the element is not present, the Set is unchanged.

```java
s = [ 1, 2, 3 ].toSet();
s.remove( 2 );
writeOutput( s.size() );

```

Result: 2

### Using the delete alias

`delete` is an alias for `remove`.

```java
s = [ 1, 2, 3 ].toSet();
s.delete( 3 );
writeOutput( s.size() );

```

Result: 2
