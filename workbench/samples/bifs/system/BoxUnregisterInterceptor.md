### Unregister a global interceptor

The same reference that was registered is passed to unregister it.

```java
interceptor = new MyInterceptor()
boxRegisterInterceptor( interceptor )
result = boxUnregisterInterceptor( interceptor )
writeOutput( result )
```

Result: true

### Unregister from specific states only

```java
boxUnregisterInterceptor( interceptor, [ "onOrderPlaced" ] )
```

Result: the interceptor stops listening to onOrderPlaced but stays registered for its other states; no output
