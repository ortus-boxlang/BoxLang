### Tag Example

 


```java
<!--- Make a query. ---> 
 <bx:set myQuery = queryNew( "" ) > 
  <!--- Create an array. ---> 
 <bx:set FastFoodArray = arrayNew( 1 ) > 
 <bx:set FastFoodArray[ 1 ] = "French Fries" > 
 <bx:set FastFoodArray[ 2 ] = "Hot Dogs" > 
 <bx:set FastFoodArray[ 3 ] = "Fried Clams" > 
 <bx:set FastFoodArray[ 4 ] = "Thick Shakes" > 
 <!--- Use the array to add a column to the query. ---> 
 <bx:set nColumnNumber = queryAddColumn( myQuery, "FastFood", "VarChar", FastFoodArray ) > 
 <bx:dump var="#myQuery#"/> 
```

Result: 

### member syntax example

add a column to a query using member syntax

<a href="https://try.boxlang.io/?code=eJxLys%2FPLlawVSgsTS2q9Est11BQykzRKcksyUlV0gGy80pS01OLdMoSi5IzEouUFDStuZJAWvQSU1Kc83NKc%2FOAOhJLSzLyi0DqYep0FKJjQWrLizJLUl1Kcws0FMDaQGIAukUlgg%3D%3D" target="_blank">Run Example</a>

```java
books = queryNew( "id,title", "integer,varchar" );
books.addColumn( "author", "varchar", [] );
writeDump( books );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUoLE0tqvRLLddQUEpMTNRJSkpSUtC05goECTumpATlA2UKgSphYsGpJc6pOTlgQR2wHiUgZahnqIRLCchIsBIjckw2ImyyEZrJzvk5pbl5MEXJyclARTmZxSUh%2BY5FRYmVQJ8a6xnqGIN14daXkpICMjwzryQ1PbUIwwgToBEmcCPKizJLUl1KcwvAuvWc%2FX1Cff18PINDQJIAVuJqfw%3D%3D" target="_blank">Run Example</a>

```java
qry = queryNew( "aaa,bbb" );
QueryAddRow( qry );
QuerySetCell( qry, "aaa", "1.1" );
QuerySetCell( qry, "bbb", "1.2" );
QueryAddRow( qry );
QuerySetCell( qry, "aaa", "2.1" );
QuerySetCell( qry, "bbb", "2.2" );
QueryAddColumn( qry, "ccc", listToArray( "3.1,3.2" ) );
QueryAddColumn( qry, "ddd", "integer", listToArray( "4.1,4.2" ) );
writeDump( qry.COLUMNLIST );

```


