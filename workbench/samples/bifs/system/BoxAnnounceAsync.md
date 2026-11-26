### Announce an interception event asynchronously

The call returns a future that completes when the event has been announced.

```java
boxRegisterInterceptionPoints( "onAsync" )
future = boxAnnounceAsync( "onAsync", { data : "async" } )
future.get()
writeOutput( "announced" )
```

Result: announced

### Announce to the request pool

The request pool is only available during a request with an application listener.

```java
boxRegisterInterceptionPoints( "onMyRequestEvent", "request" )
future = boxAnnounceAsync( "onMyRequestEvent", { id : 1 }, "request" )
future.get()
```

Result: the event is announced to the request interceptor pool; no output
