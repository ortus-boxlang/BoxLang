### Register interception points globally

```java
result = boxRegisterInterceptionPoints( [ "onApplicationReady", "onOrderPlaced" ] )
writeOutput( result )
```

Result: true

### Register a single point in the request pool

A single name can be passed instead of an array.

```java
boxRegisterInterceptionPoints( "onOrderShipped", "request" )
```

Result: the point is registered in the request interceptor pool; no output
