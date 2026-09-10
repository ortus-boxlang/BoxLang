### Make an HTTP request

```java
result = http( method: "GET", url: "https://httpbin.org/get" );
writeOutput( result.statusCode );

```

Result: 200

### POST with body

```java
result = http( method: "POST", url: "https://httpbin.org/post", body: "hello" );
writeOutput( result.statusCode );

```

Result: 200
