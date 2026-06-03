### Check if the current thread has been interrupted

```java
interrupted = isThreadInterrupted();
writeOutput( isBoolean( interrupted ) );

```

Result: false

### Returns false for a normal running thread

```java
writeOutput( isThreadInterrupted() );

```

Result: false
