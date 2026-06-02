### Combine two arrays element-by-element

Each result element is an array of the paired values.

```java
names = [ "Alice", "Bob" ];
ages = [ 30, 25 ];
result = names.zip( ages );
writeOutput( result[ 1 ].toString() );

```

Result: [Alice, 30]

### Zipped array length matches shortest input

```java
letters = [ "a", "b", "c" ];
numbers = [ 1, 2 ];
result = letters.zip( numbers );
writeOutput( result.len() );

```

Result: 2

### Using the global function form

```java
result = arrayZip( [ 1, 2 ], [ "x", "y" ] );
writeOutput( result.toString() );

```

Result: [[1, x], [2, y]]
