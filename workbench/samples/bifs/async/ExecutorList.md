### List all registered executors

Returns an array with the names of the registered executors.

```java
executorNew( "pool1", "fixed", 2 )
executorNew( "pool2", "cached" )
names = executorList()
writeOutput( names.contains( "pool1" ) & "," & names.contains( "pool2" ) )
```

Result: true,true

### The default executors are listed

```java
writeOutput( executorList().contains( "io-tasks" ) )
```

Result: true
