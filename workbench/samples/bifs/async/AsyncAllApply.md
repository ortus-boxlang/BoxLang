### Apply a function to each item in parallel

Accepts an array of items and a function, applying the function to each item in parallel.

```java
items = [ 1, 2, 3, 4, 5 ];
results = allApply( items, ( n ) => n * n ).get();
writeOutput( results.toString() );

```

Result: [1, 4, 9, 16, 25]

### Process a struct of items

```java
data = { a: 10, b: 20, c: 30 };
results = allApply( data, ( v ) => v * 2 ).get();
writeOutput( results.len() );

```

Result: 3

### Using a named executor

```java
executorNew( "myPool", "fixed", 4 );
results = allApply( [ 1, 2, 3 ], ( n ) => n + 1, "myPool" ).get();
writeOutput( results.toString() );

```

Result: [2, 3, 4]
