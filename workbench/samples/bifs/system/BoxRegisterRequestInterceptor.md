### Register a request-scoped interceptor class

The class (for example MyRequestInterceptor.bx) defines methods named after the events it listens to.

```java
interceptor = new MyRequestInterceptor()
result = boxRegisterRequestInterceptor( interceptor )
writeOutput( result )
```

Result: true

### Register a request-scoped closure

```java
boxRegisterRequestInterceptor( ( data ) => false, [ "onRequestEnd" ] )
```

Result: the closure listens to onRequestEnd for the application request only; no output
