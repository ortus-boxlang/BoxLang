### ID column is in given query



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCppc5UWZJan%2BpSUFpSUaEGXO%2BTmluXmuFZnFJcUaCrkQM3RAGoHqFTQB8ycchw%3D%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" )
writeOutput( queryColumnExists( myQuery, "ID" ) )

```

Result: true

### Whereas "gender" is not



<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKni46eYm5qTqJ6alKCppc5UWZJan%2BpSUFpSUaEGXO%2BTmluXmuFZnFJcUaCrkQM3QUlNJT81JSi4B6FDQBcYMebw%3D%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "ID,name,age" )
writeOutput( queryColumnExists( myQuery, "gender" ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwsTS2qVLBVKATRfqnlGgpKyaXFJZ4uOiDKLzE3VUlBk6u8KLMk1aU0t0ADotA5P6c0N8%2B1IrO4pFhDIRdiio6CUmI6SDkJGpAsUdAEAO2WMQ4%3D" target="_blank">Run Example</a>

```java
myQuery = queryNew( "custID,custName" )
writeDump( queryColumnExists( myQuery, "age" ) )
writeDump( queryColumnExists( myQuery, "custName" ) )

```


