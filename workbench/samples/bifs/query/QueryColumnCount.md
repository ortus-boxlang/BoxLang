### Output number of query columns



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCppc5UWZJan%2BpSUFpSUaEGXO%2BTmluXnO%2BaV5QJFcqBGaCpoALDsa8w%3D%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" )
writeOutput( queryColumnCount( myQuery ) )

```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLKpUsFUoLE0tqvRLLddQUEpMTNRJSkrSSU5O1klJSdFJTU1VUtDkCgSpcExJCcoHKioEaoIKBaeWOKfm5IDFdMC6lYBUcXExNk06CsZ49WkYamLVZQgULS%2FKLEl1Kc0t0IC41jk%2FpzQ3zzm%2FNK8E6iAFTQA7RkD3" target="_blank">Run Example</a>

```java
qry = queryNew( "aaa,bbb,ccc,ddd,eee" )
QueryAddRow( qry )
QuerySetCell( qry, "aaa", "sss" )
QueryAddRow( qry, 3 )
QuerySetCell( qry, "aaa", (1) )
QueryAddRow( qry, 1 )
writeDump( queryColumnCount( qry ) )

```

### Member function

```java
q = queryNew( "a,b" )
writeOutput( q.columnCount() )

```

Result: 2
