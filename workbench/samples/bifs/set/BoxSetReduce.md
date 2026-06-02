### Sum all elements with reduce

The callback receives an accumulator and the current value. The third argument is the initial accumulator value.

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
total = s.reduce( ( Any acc, Any v ) => acc + v, 0 );
writeOutput( total );

```

Result: 15

### Build a comma-separated string

```java
s = setNew( type="linked", values=[ "apple", "banana", "cherry" ] );
result = s.reduce( ( Any acc, Any v ) => listAppend( acc, v ), "" );
writeOutput( result );

```

Result: apple,banana,cherry
