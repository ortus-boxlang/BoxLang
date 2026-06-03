### Convert an Array to a Set

Calling `.toSet()` on an Array deduplicates its elements and returns a BoxSet.

```java
s = [ 1, 2, 2, 3 ].toSet();
writeOutput( s.size() );

```

Result: 3

### Convert an Array to a linked (insertion-ordered) Set

Pass a type argument to control the backing implementation.

```java
s = [ "c", "a", "b", "a" ].toSet( "linked" );
writeOutput( s.toArray().toString() );

```

Result: [c, a, b]

### Convert an Array to a sorted Set

```java
s = [ 9, 1, 5, 3 ].toSet( "sorted" );
writeOutput( s.toArray().toString() );

```

Result: [1, 3, 5, 9]

### Split a comma-delimited string into a Set

`listToSet()` splits the string on the default delimiter and deduplicates.

```java
s = "a,b,c,a".listToSet();
writeOutput( s.size() );

```

Result: 3

### Split a string with a custom delimiter

```java
s = "a|b|c|b".listToSet( delimiter="|" );
writeOutput( s.size() );

```

Result: 3

### Split with a custom delimiter and preserve insertion order

```java
s = "a|b|c|b".listToSet( delimiter="|", type="linked" );
writeOutput( s.toArray().toString() );

```

Result: [a, b, c]

### Convert a Query to a Set of row structs

Each row becomes a Struct element in the Set.

```java
q = queryNew( "name,age", "varchar,integer", [ [ "Alice", 30 ], [ "Bob", 25 ] ] );
s = q.toSet();
writeOutput( s.size() );

```

Result: 2

### Get distinct column values as a Set

Use `.columnData()` to extract a column as an Array first, then call `.toSet()` to deduplicate.

```java
q = queryNew( "name", "varchar", [ [ "Alice" ], [ "Bob" ], [ "Alice" ] ] );
s = q.columnData( "name" ).toSet();
writeOutput( s.size() );

```

Result: 2
