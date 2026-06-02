### Get executor status information

```java
executorNew( "statusPool", "fixed", 4 );
status = executorStatus( "statusPool" );
writeOutput( status.containsKey( "name" ) );

```

Result: true

### Status includes pool type and thread count

```java
executorNew( "infoPool", "fixed", 8 );
status = executorStatus( "infoPool" );
writeOutput( status.name() & "," & status.type() );

```

Result: infoPool,FIXED
