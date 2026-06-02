### Check if an array has no elements

```java
empty = [];
writeOutput( empty.isEmpty() );

```

Result: true

### Returns false for a non-empty array

```java
items = [ "apple", "banana" ];
writeOutput( items.isEmpty() );

```

Result: false

### Using the global function form

```java
writeOutput( arrayIsEmpty( [] ) & "," & arrayIsEmpty( [ 1 ] ) );

```

Result: true,false
