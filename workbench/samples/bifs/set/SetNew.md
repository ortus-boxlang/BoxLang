### Create an empty default Set

Creates a new empty hash-based Set (no ordering, fastest lookup).

```java
s = setNew();
writeOutput( s.size() );

```

Result: 0

### Create a linked (insertion-ordered) Set seeded with values

Duplicate values are automatically removed.

```java
s = setNew( type="linked", values=[ "c", "a", "b", "a" ] );
writeOutput( s.toArray().toString() );

```

Result: [c, a, b]

### Create a sorted Set

Elements are kept in natural ascending order at all times.

```java
s = setNew( type="sorted", values=[ 9, 1, 5, 3 ] );
writeOutput( s.toArray().toString() );

```

Result: [1, 3, 5, 9]

### Create a case-sensitive Set

By default Sets are case-insensitive. Pass `caseSensitive=true` to treat different cases as distinct elements.

```java
s = setNew( values=[ "Hello", "hello", "HELLO" ], caseSensitive=true );
writeOutput( s.size() );

```

Result: 3

### Default behavior is case-insensitive

```java
s = setNew( values=[ "Hello", "hello", "HELLO" ] );
writeOutput( s.size() );

```

Result: 1
