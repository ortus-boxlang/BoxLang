### Remove all elements from a Set

After calling `clear()` the Set is empty but still usable.

```java
s = [ 1, 2, 3 ].toSet()
before = s.isEmpty()
s.clear()
after = s.isEmpty()
writeOutput( before & "," & after )

```

Result: false,true

### Clear is chainable

`clear()` returns the same Set, so you can add new values right away.

```java
s = [ 1, 2, 3 ].toSet()
s.clear().add( "x" )
writeOutput( s.size() )

```

Result: 1
