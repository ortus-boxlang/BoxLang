### Creating sets using the function `setNew`

```java
// Create a default set (hash-based, no ordering)
mySet = setNew();

// Create a linked set which will maintain insertion order
mySet = setNew( type="linked" );

// Create a sorted set which will keep elements in natural order
mySet = setNew( type="sorted" );

// Create a set seeded with values (duplicates removed)
mySet = setNew( values=[ 1, 2, 2, 3 ] );

// Create a case-sensitive set
mySet = setNew( values=[ "Hello", "hello", "HELLO" ], caseSensitive=true );
```


### Creating sets using literal syntax

```java
// Create a set with values
mySet = set{ 1, 2, 3 };

// Create an empty set
emptySet = set{};

// Spread an array into a set
arr = [ 3, 4, 5 ];
s = set{ 1, 2, ...arr };

// Spread another set
other = set{ 2, 3 };
s = set{ 1, ...other, 4 };

// Spread a range
s = set{ ...(1..5) };
```

### Creating sets from varargs

```java
// setOf deduplicates automatically
s = setOf( 1, 2, 2, 3 );
// Result: Set with 3 elements
```

### Converting arrays to sets

```java
// Convert array to default set
s = [ 1, 2, 2, 3 ].toSet();

// Convert to linked (insertion-ordered) set
s = [ "c", "a", "b", "a" ].toSet( "linked" );

// Convert to sorted set
s = [ 9, 1, 5, 3 ].toSet( "sorted" );
```

### Converting strings to sets

```java
// Split comma-delimited string to set
s = "a,b,c,a".listToSet();

// Custom delimiter with type
s = "a|b|c|b".listToSet( delimiter="|", type="linked" );
```

### Set membership and mutation

```java
s = setNew();

// Add elements (add and append are aliases)
s.add( "apple" );
s.append( "banana" );

// Test membership (contains and has are aliases)
s.contains( "apple" );    // true
s.has( "banana" );        // true

// Remove elements (remove and delete are aliases)
s.remove( "apple" );
s.delete( "banana" );

// Size (size, len, length are aliases)
s.size();
s.len();
s.length();
```

### Set algebra

```java
a = set{ 1, 2, 3 };
b = set{ 3, 4, 5 };

// Union - all unique elements
u = a.union( b );        // {1, 2, 3, 4, 5}
u = a + b;               // operator shorthand

// Intersection - common elements
i = a.intersection( b ); // {3}
i = a * b;               // operator shorthand

// Difference - in A but not B
d = a.difference( b );   // {1, 2}
d = a - b;               // operator shorthand

// Symmetric difference - in either but not both
x = a.symmetricDifference( b ); // {1, 2, 4, 5}
x = a ^ b;                      // operator shorthand
```

### Functional operations

```java
s = [ 1, 2, 3, 4, 5 ].toSet();

// Map - transform elements
doubled = s.map( v -> v * 2 );

// Filter - keep matching
evens = s.filter( v -> v % 2 == 0 );

// Reduce - combine to single value
total = s.reduce( (acc, v) -> acc + v, 0 );

// Predicates
s.every( v -> v > 0 );     // true
s.some( v -> v > 4 );      // true
s.none( v -> v < 0 );      // true

// Find first match
found = s.find( v -> v > 2 );
```

### Converting sets back

```java
s = setNew( type="linked", values=[ "a", "b", "c" ] );

// To array
arr = s.toArray();

// To list string
list = s.toList();
list = s.toList( "-" );    // custom delimiter
```

### Struct key/value sets

```java
data = { name: "Luis", age: 42, email: "x@y.z" };

// Get keys as a set
keys = data.keySet();

// Get values as a set (deduplicated)
values = data.valueSet();
```
