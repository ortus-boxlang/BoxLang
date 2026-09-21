### Keep only elements that do NOT satisfy a predicate

The complement of `filter`.

```java
numbers = [ 1, 2, 3, 4, 5 ];
odds = numbers.reject( ( n ) => n % 2 == 0 );
writeOutput( odds.toString() );

```

Result: [1, 3, 5]

### Reject short strings

```java
words = [ "cat", "elephant", "ox", "deer" ];
long = words.reject( ( w ) => len( w ) <= 3 );
writeOutput( long.toString() );

```

Result: [elephant, deer]

### Using the global function form

```java
result = arrayReject( [ 10, 20, 30, 40 ], ( n ) => n > 25 );
writeOutput( result.toString() );

```

Result: [10, 20]
