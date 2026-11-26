### Register a closure as a global interceptor

A closure or lambda must be given at least one state to listen to.

```java
boxRegisterInterceptionPoints( "onOrderPlaced" )
result = boxRegisterInterceptor( ( data ) => false, [ "onOrderPlaced" ] )
writeOutput( result )
```

Result: true

### A closure without states throws an error

```java
try {
    boxRegisterInterceptor( ( data ) => false )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Closures/Lambdas need at least one state to listen to
