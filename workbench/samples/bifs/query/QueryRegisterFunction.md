### Register a function available within query operations

```java
queryRegisterFunction( "doubleIt", ( value ) => value * 2 );
q = queryNew( "n", "integer", [ [ 5 ], [ 10 ] ] );
result = q.map( ( row ) => doubleIt( row.n ) );
writeOutput( result[ 1 ] );

```

Result: 10
