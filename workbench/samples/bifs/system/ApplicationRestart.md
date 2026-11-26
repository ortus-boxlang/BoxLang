### Restart the current application

Requests a restart of the running application and returns true.

```java
result = applicationRestart()
writeOutput( result )
```

Result: true

### Restart from an admin-triggered request

A common pattern is to restart only when a URL flag is present.

```java
if ( url.restartApp == "true" ) {
    applicationRestart()
}
```

Result: the application restarts when the request is made with ?restartApp=true; nothing happens otherwise
