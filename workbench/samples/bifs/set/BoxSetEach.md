### Iterate over every element in a Set

The callback receives each value in turn.

```java
s = setNew( type="linked", values=[ "a", "b", "c" ] );
s.each( ( Any value ) => {
	writeOutput( value & " " );
} );

```

Result: a b c

### Accumulate with each

```java
s = [ 1, 2, 3, 4, 5 ].toSet();
counter = [ 0 ];
s.each( ( Any v ) => {
	counter[ 1 ] += v;
} );
writeOutput( counter[ 1 ] );

```

Result: 15
