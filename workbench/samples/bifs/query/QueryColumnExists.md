### ID column is in given query



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCprWXOVFmSWp%2FqUlBaUlGhB1zvk5pbl5rhWZxSXFGgq5EEN0QDqBGkBaAERdHQc%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" );
writeOutput( queryColumnExists( myQuery, "ID" ) );

```

Result: true

### Whereas "gender" is not



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCprWXOVFmSWp%2FqUlBaUlGhB1zvk5pbl5rhWZxSXFGgq5EEN0FJTSU%2FNSUouAmkDaAMdmHu8%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" );
writeOutput( queryColumnExists( myQuery, "gender" ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKyaXFJZ4uOiDKLzE3VUlB05qrvCizJNWlNLdAA6LSOT%2BnNDfPtSKzuKRYQyEXYoyOglJiOkg9KTqQrAFpAwChpTHJ" target="_blank">Run Example</a>

```java
myQuery = queryNew( "custID,custName" );
writeDump( queryColumnExists( myQuery, "age" ) );
writeDump( queryColumnExists( myQuery, "custName" ) );

```


