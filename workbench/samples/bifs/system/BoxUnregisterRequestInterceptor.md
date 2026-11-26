### Unregister a request-scoped interceptor

```java
interceptor = new MyRequestInterceptor()
boxRegisterRequestInterceptor( interceptor )
result = boxUnregisterRequestInterceptor( interceptor )
writeOutput( result )
```

Result: true

### Unregister from specific states only

```java
boxUnregisterRequestInterceptor( interceptor, [ "onRequestEnd" ] )
```

Result: the interceptor stops listening to onRequestEnd; no output
