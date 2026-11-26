### Remove duplicate values from an array

```java
items = [ "apple", "banana", "apple", "cherry", "banana" ]
result = items.unique()
writeOutput( result.len() )

```

Result: 3

### Case-insensitive deduplication (default)

```java
words = [ "Hello", "hello", "HELLO", "World" ]
result = words.unique()
writeOutput( result.len() )

```

Result: 2

### Using the global function form

```java
result = arrayUnique( [ 1, 2, 2, 3, 3, 3 ] )
writeOutput( result.toString() )

```

Result: [1, 2, 3]

### Case sensitive deduplication

Pass true to compare strings case sensitively.

```java
words = [ "Hello", "hello", "World" ]
writeOutput( words.unique( true ).len() )
```

Result: 3
