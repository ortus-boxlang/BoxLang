### Create a query and output the column list



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCppc5UWZJan%2BpSUFpSUaEGXO%2BTmluXk%2BmcVAgVyoCZoKmgAPqRqG" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" )
writeOutput( queryColumnList( myQuery ) )

```

Result: ID,name,age

### Using a member function



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCppc5UWZJan%2BpSUFpSUaCrkQDXrJ%2BTmluXk%2BmcUlGpoKmgBxDxhe" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" )
writeOutput( myQuery.columnList() )

```

Result: ID,name,age

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUoLE0tqvRLLddQUEpMTFRS0ORKLCoCigPJRIi4IUQsGsiIBUooJSpBuEYQbhKUawzhJitxBYKMdExJcc7PKc3N01AoLKrUAapLSlLSARkLNA67iuTkZLiK8qLMklSX0twCDYgLISp9MotLwKoVNBU0ASlAOf8%3D" target="_blank">Run Example</a>

```java
qry = queryNew( "aaa" )
arr = arrayNew( 1 )
arr[ 1 ] = "a"
arr[ 2 ] = "b"
arr[ 3 ] = "c"
QueryAddColumn( qry, "bbb", arr )
QueryAddColumn( qry, "ccc", arr )
writeDump( queryColumnList( qry ) )

```


