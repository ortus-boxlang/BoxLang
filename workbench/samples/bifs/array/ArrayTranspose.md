### Swap rows and columns in a 2D array

```java
matrix = [
    [ 1, 2, 3 ],
    [ 4, 5, 6 ]
];
result = matrix.transpose();
writeOutput( result.len() & "," & result[ 1 ].len() );

```

Result: 3,2

### Transposed result

```java
matrix = [
    [ 1, 2 ],
    [ 3, 4 ]
];
result = matrix.transpose();
writeOutput( result[ 1 ].toString() );

```

Result: [1, 3]

### Using the global function form

```java
result = arrayTranspose( [ [ "a", "b" ], [ "c", "d" ] ] );
writeOutput( result[ 2 ].toString() );

```

Result: [b, d]
