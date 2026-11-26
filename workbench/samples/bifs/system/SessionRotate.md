### Rotate the current session

A new session ID is created, the session data is copied to it and the old session is invalidated. Use it after a login to prevent session fixation.

```java
session.user = "luis"
sessionRotate()
writeOutput( session.user )
```

Result: luis

### Rotate after authentication

```java
if ( authenticate( username, password ) ) {
    sessionRotate()
    session.loggedIn = true
}
```

Result: the session ID changes and the session data is retained
