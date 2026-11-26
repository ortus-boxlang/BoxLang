### Stop the current application

Stops the application and returns true. Subsequent requests start the application again.

```java
result = applicationStop()
writeOutput( result )
```

Result: true

### Stop the application on demand

```java
if ( url.shutdown == "true" ) {
    applicationStop()
}
```

Result: the application is stopped when the request is made with ?shutdown=true; nothing happens otherwise
