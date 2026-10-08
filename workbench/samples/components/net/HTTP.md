### Script Syntax



<a href="https://try.boxlang.io/?code=eJwtjEEOwiAQAM%2Fyis2e9GC5mjbcNH7AD1DcFhMQCotojH8XU08zh8mMz94yR%2FDENlwVnk8XBGN1ysQKC0%2F7A0JJTuEvy72UtdZuDmF21JngJUKiXFyLVyK8xWZcr1En7eGuPSlcEPgVm7QZwkO70txM3uEgPqKmG9Ox%2BLj972A3iC9hPzS%2B" target="_blank">Run Example</a>

```java
bx:http method="GET" charset="utf-8" url="https://www.google.com/" result="result" {
	bx:httpparam name="q" type="url" value="bx";
}
writeDump( result );

```


### Alternate Script Syntax




```java
httpService = new http( method="GET", charset="utf-8", url="https://www.google.com/" );
httpService.addParam( name="q", type="url", value="bx" );
result = httpService.send().getPrefix();
writeDump( result );

```


### BX:HTTP Tag Syntax




```java
<bx:http result="result" method="GET" charset="utf-8" url="https://www.google.com/">
    <bx:httpparam name="q" type="url" value="bx">
</bx:http>
<bx:dump var="#result#">
```

### Binary Streaming (New in 1.19.0)

`onBinaryChunk` receives the raw bytes of a streaming response, such as audio, exactly as they are read from the network. The callback gets `(bytes, info)`. `info` has `chunkNumber`, `totalBytes`, `result` and `httpClient`, and `headers` on the first chunk only.

```java
bx:http url="https://example.com/audio.mp3" method="GET" result="audio"
	onBinaryChunk=function( bytes, info ) {
		if ( info.chunkNumber == 1 ) {
			writeOutput( "Content-Type: " & info.headers[ "Content-Type" ] & "<br>" );
		}
		writeOutput( "chunk " & info.chunkNumber & ": " & arrayLen( bytes ) & " bytes<br>" );
	} {}
writeOutput( "Total: " & audio.totalBytes & " bytes in " & audio.chunkCount & " chunks" );

```


### Binary Streaming To A File (New in 1.19.0)

The body is not accumulated in memory, so a large download can be written straight to disk.

```java
target = getTempDirectory() & "download.bin";
out = createObject( "java", "java.io.FileOutputStream" ).init( target );
try {
	bx:http url="https://example.com/audio.mp3" method="GET" result="audio"
		onBinaryChunk=function( bytes, info ) {
			out.write( bytes );
		} {}
} finally {
	out.close();
}
writeOutput( "Saved " & fileInfo( target ).size & " of " & audio.totalBytes & " bytes" );

```


### Binary Streaming With Headers And A Body (New in 1.19.0)

Params, headers and request bodies work as usual. While streaming, `timeout` is an idle timeout: the longest wait for the response headers or between received bytes.

```java
sent = 0;
bx:http url="https://api.example.com/v1/speech" method="POST" timeout=30 result="speech"
	onBinaryChunk=function( bytes, info ) {
		sent += arrayLen( bytes );
		// Forward the bytes to your own client here
	} {
	bx:httpparam type="header" name="Authorization" value="Bearer #apiKey#";
	bx:httpparam type="body" value='{"text":"Hello from BoxLang"}';
}
writeOutput( "Sent " & sent & " bytes, status " & speech.statusCode );

```


### Binary Streaming With Early Stop (New in 1.19.0)

Return an explicit `false` to stop. The connection is closed immediately and `streamCompleted` is `false`. Any other return value keeps streaming.

```java
bx:http url="https://example.com/audio.mp3" method="GET" result="audio"
	onBinaryChunk=function( bytes, info ) {
		// Stop after three chunks
		return info.chunkNumber < 3;
	} {}
writeOutput( "completed: " & audio.streamCompleted & ", chunks: " & audio.chunkCount );

```


### Binary Streaming Errors And Timeouts (New in 1.19.0)

A non 2xx status never reaches the callback. It is reported as `HTTP <status>: <body>` through `onError`. A stream that stays silent for longer than `timeout` seconds is aborted and reported as a `408` timeout.

```java
bx:http url="https://api.example.com/v1/speech" method="GET" result="failed"
	onBinaryChunk=function( bytes, info ) {
		// Not called for a 401
	}
	onError=function( error, httpResult ) {
		writeOutput( "error: " & error.message & "<br>" );
	} {}
writeOutput( "status: " & failed.statusCode & "<br>" );

bx:http url="https://example.com/slow-stream" method="GET" timeout=10 result="stalled"
	onBinaryChunk=function( bytes, info ) {
		writeOutput( "got " & arrayLen( bytes ) & " bytes<br>" );
	}
	onError=function( error, httpResult ) {
		writeOutput( "error: " & error.message & "<br>" );
	} {}
writeOutput( "status: " & stalled.statusCode );

```


### Stopping A Server-Sent Events Stream (New in 1.19.0)

Returning `false` from an SSE `onChunk` stops the stream. The same idle timeout and error reporting apply.

```java
bx:http url="https://api.example.com/events" method="GET" sse=true timeout=30 result="events"
	onChunk=function( event ) {
		if ( event.data == "[DONE]" ) {
			return false;
		}
		writeOutput( "event: " & event.data & "<br>" );
	} {}
writeOutput( "completed: " & events.streamCompleted );

```


### Binary Streaming Tag Syntax (New in 1.19.0)

In tag syntax, define the callbacks as functions and pass them as attributes.

```java
<bx:script>
	total = 0;
	function handleChunk( bytes, info ) {
		total += arrayLen( bytes );
		return true;
	}
	function handleError( error, httpResult ) {
		writeOutput( "error: " & error.message );
	}
</bx:script>

<bx:http url="https://example.com/audio.mp3" method="GET" result="audio"
	onBinaryChunk="#handleChunk#" onError="#handleError#" />

<bx:output>Received #total# bytes in #audio.chunkCount# chunks, completed: #audio.streamCompleted#</bx:output>
```

