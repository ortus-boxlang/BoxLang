### Remove all elements from a Set

After calling `clear()` the Set is empty but still usable.

```java
s = [ 1, 2, 3 ].toSet();
before = s.isEmpty();
s.clear();
after = s.isEmpty();
writeOutput( before & "," & after );

```

Result: false,true
