### Create a new future for async execution

```java
future = futureNew( () => 42 );
writeOutput( future.get() );

```

Result: 42

### Create a completed future with a value

```java
future = futureNew( "already done" );
writeOutput( future.isDone() & "," & future.get() );

```

Result: true,already done

### Create an incomplete future and complete it later

```java
future = futureNew();
future.complete( "later" );
writeOutput( future.get() );

```

Result: later
