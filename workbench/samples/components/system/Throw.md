### Throw a custom exception

Raises a developer-defined exception that can be caught by a try/catch block.

```java
<bx:throw type="ValidationException" message="Email is required">

```

### Throw with detail and error code

```java
<bx:throw
    type="DatabaseError"
    message="Failed to save record"
    detail="Constraint violation on users.email column"
    errorCode="DB_001"
>

```

### Throw with extended info

```java
<bx:throw
    type="AuthException"
    message="Invalid token"
    extendedInfo="#{ userId: 42, token: 'abc123' }#"
>

```

### Rethrow an existing exception object

```java
try {
    riskyOperation();
} catch ( any e ) {
    <bx:throw object="#e#" message="Wrapped: #e.message#">
}

```
