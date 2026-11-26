### Invalidate the current session

The session scope is cleared and the session identifiers are no longer valid.

```java
session.cart = [ 1, 2 ]
sessionInvalidate()
writeOutput( structKeyExists( session, "cart" ) )
```

Result: false

### Log a user out

```java
function logout() {
    sessionInvalidate()
    location( url = "/login", addToken = false )
}
```

Result: the session is cleared and the user is redirected to /login
