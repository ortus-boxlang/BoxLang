### Collect a stream of structs into a Query

Requires a template query to define the column structure.

```java
template = queryNew( "id,name" );
data = [
    { id: 1, name: "Alice" },
    { id: 2, name: "Bob" }
];
q = data.toStream().toBXQuery( template );
writeOutput( q.recordCount );

```

Result: 2

### Query with multiple columns

```java
template = queryNew( "id,product,price" );
data = [
    { id: 1, product: "Widget", price: 9.99 },
    { id: 2, product: "Gadget", price: 19.99 },
    { id: 3, product: "Doohickey", price: 4.99 }
];
q = data.toStream().toBXQuery( template );
writeOutput( q.recordCount & " records" );

```

Result: 3 records
