### Get executor status information

```java
executorNew( "statusPool", "fixed", 4 )
status = executorStatus( "statusPool" )
writeOutput( status.containsKey( "name" ) )
```

Result: true

### Status includes the name and pool size

```java
executorNew( "infoPool", "fixed", 8 )
status = executorStatus( "infoPool" )
writeOutput( status.name & "," & status.maxThreads )
```

Result: infoPool,8

### Status of all executors

Without a name, a struct of every executor and its statistics is returned.

```java
all = executorStatus()
writeOutput( all.keyExists( "io-tasks" ) )
```

Result: true
