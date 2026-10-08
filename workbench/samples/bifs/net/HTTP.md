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

### Stream a binary response (New in 1.19.0)

```java
total = 0;
result = http( "https://example.com/audio.mp3" )
	.timeout( 30 )
	.onBinaryChunk( ( bytes, info ) => {
		total += arrayLen( bytes );
	} )
	.send();
writeOutput( total == result.totalBytes );

```

Result: true

### Stop a stream early (New in 1.19.0)

```java
result = http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {
		// An explicit false stops the stream and closes the connection
		return false;
	} )
	.send();
writeOutput( result.streamCompleted );

```

Result: false

### Handle a streaming error (New in 1.19.0)

```java
http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {
		// Not called for a non 2xx status
	} )
	.onError( ( error, httpResult ) => {
		// For example: HTTP 401: {"error":"bad key"}
		writeOutput( error.message );
	} )
	.send();

```

