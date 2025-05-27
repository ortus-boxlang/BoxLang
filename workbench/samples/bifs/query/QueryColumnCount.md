### Output number of query columns



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCprWXOVFmSWp%2FqUlBaUlGhB1zvk5pbl5zvmleUCRXKgZmiC1AHidG3M%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" );
writeOutput( queryColumnCount( myQuery ) );

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUoLE0tqvRLLddQUEpMTNRJSkrSSU5O1klJSdFJTU1VUtC05goEKXFMSQnKB6oqBOqCiQWnljin5uSABXXA%2BpWAVHFxMVZtOgrG%2BHVqGGpi12cIEi4vyixJdSnNLdCAuNk5P6c0N885vzSvBOoqkCoAFHxCng%3D%3D" target="_blank">Run Example</a>

```java
qry = queryNew( "aaa,bbb,ccc,ddd,eee" );
QueryAddRow( qry );
QuerySetCell( qry, "aaa", "sss" );
QueryAddRow( qry, 3 );
QuerySetCell( qry, "aaa", (1) );
QueryAddRow( qry, 1 );
writeDump( queryColumnCount( qry ) );

```


