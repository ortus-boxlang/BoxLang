### Convert a Set to an Array

```java
s = setNew( type="linked", values=[ "a", "b", "c" ] );
arr = s.toArray();
writeOutput( arr.len() & "," & arr[ 1 ] );

```

Result: 3,a

### Sorted Set produces a sorted Array

```java
s = setNew( type="sorted", values=[ 9, 1, 5, 3 ] );
arr = s.toArray();
writeOutput( arr[ 1 ] & "," & arr[ 4 ] );

```

Result: 1,9
