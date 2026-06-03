### Announce an interception event

```java
boxRegisterInterceptionPoints( [ "onTest" ] );
boxAnnounce( "onTest", { message: "hello" } );
writeOutput( "announced" );

```

Result: announced

### Announce with data struct

```java
boxRegisterInterceptionPoints( [ "onData" ] );
boxAnnounce( "onData", { key: "value", count: 42 } );
writeOutput( "done" );

```

Result: done
