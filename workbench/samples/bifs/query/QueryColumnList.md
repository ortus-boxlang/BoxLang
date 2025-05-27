### Create a query and output the column list



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCprWXOVFmSWp%2FqUlBaUlGhB1zvk5pbl5PpnFQIFcqBGaIKUAWvYbBg%3D%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" );
writeOutput( queryColumnList( myQuery ) );

```

Result: ID,name,age

### Using a member function



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCprWXOVFmSWp%2FqUlBaUlGgq5EB16yfk5pbl5PpnFJRqaIEUAtqoY3g%3D%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" );
writeOutput( myQuery.columnList() );

```

Result: ID,name,age

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUoLE0tqvRLLddQUEpMTFRS0LTmSiwqAkoAyUSIhCFUMBrIigXKKCUqQflGEH4SjG8M4ScD%2BYEgcx1TUpzzc0pz8zQUCosqdYAqk5KUdEBGg4zEriQ5ORmhpLwosyTVpTS3QAPiUIhSn8ziErByBU2QIgD8kjvh" target="_blank">Run Example</a>

```java
qry = queryNew( "aaa" );
arr = arrayNew( 1 );
arr[ 1 ] = "a";
arr[ 2 ] = "b";
arr[ 3 ] = "c";
QueryAddColumn( qry, "bbb", arr );
QueryAddColumn( qry, "ccc", arr );
writeDump( queryColumnList( qry ) );

```


