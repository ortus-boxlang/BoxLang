### Announce an interception event

The interception point must be registered before it is announced.

```java
boxRegisterInterceptionPoints( "onTest" )
result = boxAnnounce( "onTest", { message : "hello" } )
writeOutput( result )
```

Result: true

### Announce to a listener and read the data

```java
calls = []
boxRegisterInterceptionPoints( "onData" )
boxRegisterInterceptor( ( data ) => {
    calls.append( data.count )
    return false
}, [ "onData" ] )
boxAnnounce( "onData", { count : 42 } )
writeOutput( calls.toList() )
```

Result: 42
