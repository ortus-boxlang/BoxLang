### Announce an interception event asynchronously

```java
boxRegisterInterceptionPoints( [ "onAsync" ] );
future = boxAnnounceAsync( "onAsync", { data: "async" } );
future.get();
writeOutput( "announced" );

```

Result: announced
