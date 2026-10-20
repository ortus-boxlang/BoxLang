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

`onBinaryChunk` receives the raw bytes of a streaming response exactly as they are read from the network. There is no text decoding and no line splitting, so audio, images and other binary bodies arrive intact.

```java
total = 0;
result = http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {
		total += arrayLen( bytes );
	} )
	.send();
writeOutput( "Received " & result.totalBytes & " bytes in " & result.chunkCount & " chunks, matches: " & ( total == result.totalBytes ) );

```

Result: Received 40960 bytes in 22 chunks, matches: true

### Inspect the headers and track progress (New in 1.19.0)

The callback receives `(bytes, info)`. `info` has `chunkNumber` (1-based), `totalBytes` (so far, including this chunk), `result` and `httpClient`. The response `headers` are only present on the first chunk.

```java
http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {
		if ( info.chunkNumber == 1 ) {
			writeOutput( "Content-Type: " & info.headers[ "Content-Type" ] & "<br>" );
		}
		writeOutput( "chunk " & info.chunkNumber & ": " & arrayLen( bytes ) & " bytes, " & info.totalBytes & " so far<br>" );
	} )
	.send();

```

Result:

```
Content-Type: audio/mpeg
chunk 1: 2048 bytes, 2048 so far
chunk 2: 2048 bytes, 4096 so far
...
```

### Save a binary stream to a file (New in 1.19.0)

The body is not accumulated in memory when `onBinaryChunk` is used, so you can write a large download straight to disk. Close the stream in a `finally` block.

```java
target = getTempDirectory() & "download.bin";
out = createObject( "java", "java.io.FileOutputStream" ).init( target );
try {
	http( "https://example.com/audio.mp3" )
		.onBinaryChunk( ( bytes, info ) => {
			out.write( bytes );
		} )
		.send();
} finally {
	out.close();
}
writeOutput( fileInfo( target ).size );

```

Result: 40960

### Restream a POST response, for example text-to-speech (New in 1.19.0)

Headers, bodies and the HTTP method work exactly like any other request. Here a provider generates audio and each block is forwarded to a client as soon as it arrives. `timeout` is an idle timeout while streaming, so a long stream is never cut off as long as data keeps arriving.

```java
sent = 0;
result = http( "https://api.example.com/v1/speech" )
	.post()
	.header( "Authorization", "Bearer " & apiKey )
	.jsonBody( '{"text":"Hello from BoxLang","format":"mp3"}' )
	.timeout( 30 )
	.onBinaryChunk( ( bytes, info ) => {
		sent += arrayLen( bytes );
		// Forward the bytes to your own client here
	} )
	.send();
writeOutput( "Sent " & sent & " bytes, status " & result.statusCode );

```

Result: Sent 40960 bytes, status 200

### Stop a stream early (New in 1.19.0)

Return an explicit `false` to stop. The connection is closed immediately, so the server stops generating. Any other return value, including no return value, keeps streaming. Stopping early is not an error: the status code is unchanged and `streamCompleted` is `false`.

```java
result = http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {
		// Stop once we have at least 5000 bytes
		return info.totalBytes < 5000;
	} )
	.send();
writeOutput( "completed: " & result.streamCompleted & ", chunks: " & result.chunkCount & ", status: " & result.statusCode );

```

Result: completed: false, chunks: 3, status: 200

### Handle a non 2xx status (New in 1.19.0)

A response with a non 2xx status never reaches `onBinaryChunk`. The body is read and reported as `HTTP <status>: <body>` through `onError`, and the status and body are in the result.

```java
result = http( "https://api.example.com/v1/speech" )
	.onBinaryChunk( ( bytes, info ) => {
		// Not called for a 401
	} )
	.onError( ( error, httpResult ) => {
		writeOutput( "error: " & error.message & "<br>" );
	} )
	.send();
writeOutput( "status: " & result.statusCode & ", body: " & result.fileContent );

```

Result:

```
error: HTTP 401: {"error":"bad key"}
status: 401, body: {"error":"bad key"}
```

### Abort a stalled stream with the idle timeout (New in 1.19.0)

While streaming, `timeout` is the longest wait for the response headers or between received bytes. It does not limit the total duration. A stalled stream is aborted and reported as a `408` timeout through `onError`.

```java
result = http( "https://example.com/slow-stream" )
	.timeout( 10 )
	.onBinaryChunk( ( bytes, info ) => {
		writeOutput( "got " & arrayLen( bytes ) & " bytes<br>" );
	} )
	.onError( ( error, httpResult ) => {
		writeOutput( "error: " & error.message & "<br>" );
	} )
	.send();
writeOutput( "status: " & result.statusCode & ", completed: " & result.streamCompleted );

```

Result (the server sends 3 bytes and then goes silent):

```
got 3 bytes
error: The stream was idle for more than 10 second(s) without receiving data
status: 408, completed: false
```

### Run code when the stream finishes (New in 1.19.0)

`onComplete` runs after the last chunk. The result has `chunkCount`, `totalBytes` and `streamCompleted`.

```java
http( "https://example.com/audio.mp3" )
	.onBinaryChunk( ( bytes, info ) => {} )
	.onComplete( ( httpResult ) => {
		writeOutput( "done: " & httpResult.totalBytes & " bytes" );
	} )
	.send();

```

Result: done: 40960 bytes

### Stop a Server-Sent Events stream (New in 1.19.0)

Returning `false` from an SSE `onChunk` stops the stream too, and the same idle timeout and error reporting apply. This is useful for sentinels such as `[DONE]`.

```java
result = http( "https://api.example.com/events" )
	.sse( true )
	.timeout( 30 )
	.onChunk( ( event ) => {
		if ( event.data == "[DONE]" ) {
			return false;
		}
		writeOutput( "event: " & event.data & "<br>" );
	} )
	.send();
writeOutput( "completed: " & result.streamCompleted );

```

Result:

```
event: one
event: two
completed: false
```
