### Transform each element and flatten the result one level

Each callback result is expected to be an array, and all results are flattened into a single array.

```java
values = [ 1, 2, 3 ];
result = values.flatMap( ( v ) => [ v, v * 10 ] );
writeOutput( result.toString() );

```

Result: [1, 10, 2, 20, 3, 30]

### Expand a list of orders into individual items

```java
orders = [
    { id: 1, items: [ "apple", "banana" ] },
    { id: 2, items: [ "cherry" ] }
];
allItems = orders.flatMap( ( order ) => order.items );
writeOutput( allItems.len() );

```

Result: 3

### Using the global function form

```java
result = arrayFlatMap( [ 1, 2 ], ( v ) => [ v, v + 1 ] );
writeOutput( result.toString() );

```

Result: [1, 2, 2, 3]
