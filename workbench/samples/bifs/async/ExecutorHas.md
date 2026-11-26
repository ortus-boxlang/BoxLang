### Check if an executor exists by name

```java
executorNew( "myPool", "fixed", 4 )
writeOutput( executorHas( "myPool" ) )
```

Result: true

### Returns false for a non-existent executor

```java
writeOutput( executorHas( "nonexistent" ) )
```

Result: false

### The default executors always exist

```java
writeOutput( executorHas( "io-tasks" ) & "," & executorHas( "cpu-tasks" ) & "," & executorHas( "scheduled-tasks" ) )
```

Result: true,true,true
