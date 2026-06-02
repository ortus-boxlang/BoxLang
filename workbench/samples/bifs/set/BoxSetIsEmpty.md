### Check whether a Set has no elements

```java
s = setNew();
writeOutput( s.isEmpty() );

```

Result: true

### isEmpty returns false for a non-empty Set

```java
s = setOf( 1, 2 );
writeOutput( s.isEmpty() );

```

Result: false

### Combined with clear

```java
s = [ 1, 2 ].toSet();
before = s.isEmpty();
s.clear();
after = s.isEmpty();
writeOutput( before & "," & after );

```

Result: false,true
