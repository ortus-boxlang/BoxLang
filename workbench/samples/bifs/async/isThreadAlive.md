### Check if the current thread is alive

```java
alive = isThreadAlive();
writeOutput( isBoolean( alive ) );

```

Result: true

### Returns true when called from the main thread

```java
writeOutput( isThreadAlive() );

```

Result: true
