# HTTP Component: Streaming & Server-Sent Events (SSE) Support

## Overview

This PR adds comprehensive streaming and Server-Sent Events (SSE) support to the BoxLang HTTP component and `BoxHttpClient`. These features enable real-time data processing, chunked response handling, and native SSE event stream consumption.

## Features Added

### 1. **Chunked Streaming Support**

The HTTP component now supports processing responses in chunks as they arrive, rather than buffering the entire response in memory.

**New Attributes:**

- `onChunk` - Callback function invoked for each chunk of data received
- `onRequestStart` - Callback function invoked before the request is sent
- `onComplete` - Callback function invoked after the response is fully received
- `onError` - Callback function invoked if an error occurs

**Benefits:**

- Memory efficient for large responses
- Real-time data processing
- Progress tracking for long-running requests
- Better control over streaming data

### 2. **Server-Sent Events (SSE) Support**

Native support for consuming SSE streams with automatic event parsing according to the [SSE specification](https://html.spec.whatwg.org/multipage/server-sent-events.html).

**New Attributes:**

- `onMessage` - Syntactic sugar for `onChunk` + `sse=true` - receives parsed SSE events
- `sse` - Boolean flag to force SSE mode (auto-detected from `Content-Type: text/event-stream`)

**SSE Features:**

- Automatic event parsing (data, event type, id, retry)
- Multi-line data support
- Custom event types
- Event ID tracking with `Last-Event-ID` header
- Reconnection delay handling
- UTF-8 BOM handling

### 3. **Enhanced BoxHttpClient**

Significant improvements to the core HTTP client:

**New Methods:**

- `onChunk(Function)` - Set chunk processing callback
- `onRequestStart(Function)` - Set pre-request callback
- `onComplete(Function)` - Set completion callback
- `onError(Function)` - Set error callback
- `sse(boolean)` - Enable/force SSE mode

**Internal Improvements:**

- `processStreamingResponse()` - Handles chunked/SSE streaming
- `processSSEStream()` - SSE-specific parsing and event dispatch
- SSE event parser with spec-compliant field handling
- Automatic content-type detection for SSE streams
- Comprehensive error handling with callback integration

## Usage Examples

### Basic Streaming with onChunk

```boxlang
// Process large file download in chunks
bx:http url="https://example.com/large-file.zip"
    method="GET"
    result="downloadResult"
    onChunk=(chunkNumber, content, totalBytes, httpResult, httpClient, rawResponse) => {
        writeOutput("Chunk #chunkNumber#: #len(content)# bytes (Total: #totalBytes#)");
        // Process chunk as it arrives
    }
{}
```

### Server-Sent Events - Simple

```boxlang
// Consume SSE stream (auto-detects Content-Type: text/event-stream)
bx:http url="https://api.example.com/events"
    method="GET"
    result="sseResult"
    onMessage=(event, lastEventId, httpResult, httpClient, rawResponse) => {
        writeOutput("Received event: #event.data#");
    }
{}
```

### Server-Sent Events - Advanced

```boxlang
// Track events with IDs and custom event types
events = [];

bx:http url="https://api.example.com/notifications"
    method="GET"
    result="result"
    onMessage=(event, lastEventId, httpResult, httpClient, rawResponse) => {
        // event struct contains:
        // - data: event data (string or multi-line)
        // - event: custom event type (default: "message")
        // - id: event ID for reconnection
        // - retry: reconnection delay in milliseconds

        switch(event.event) {
            case "update":
                handleUpdate(event.data);
                break;
            case "notification":
                showNotification(event.data);
                break;
            default:
                logEvent(event);
        }

        events.append(event);
    }
{}

writeOutput("Received #events.len()# events");
```

### Force SSE Mode

```boxlang
// Force SSE parsing even if Content-Type is not text/event-stream
bx:http url="https://api.example.com/stream"
    method="GET"
    sse=true
    result="result"
    onMessage=(event, lastEventId, httpResult, httpClient, rawResponse) => {
        processEvent(event);
    }
{}
```

### Request Lifecycle Callbacks

```boxlang
// Complete control over request lifecycle
bx:http url="https://api.example.com/data"
    method="POST"
    result="result"
    onRequestStart=(requestInfo) => {
        writeOutput("Starting request...");
        writeOutput("SSE mode: #requestInfo.sse#");
    }
    onChunk=(chunkNumber, content, totalBytes, httpResult, httpClient, rawResponse) => {
        writeOutput("Processing chunk #chunkNumber#");
    }
    onComplete=(httpResult, response) => {
        writeOutput("Request completed in #httpResult.executionTime#ms");
    }
    onError=(exception, httpResult) => {
        writeOutput("Error: #exception.message#");
    }
{}
```

### Fluent API Usage (Java/BoxLang)

```java
BoxHttpClient client = httpService.getClient();

client.newRequest("https://api.example.com/stream")
    .method("GET")
    .onChunk(chunkCallback)
    .onComplete(completeCallback)
    .onError(errorCallback)
    .sse(true)
    .invoke();
```

## Callback Signatures

### onChunk / onMessage (SSE)

```boxlang
// SSE mode (onMessage)
function(event, lastEventId, httpResult, httpClient, rawResponse) {
    // event: IStruct with keys: data, event, id, retry
    // lastEventId: String - last received event ID (for reconnection)
    // httpResult: IStruct - HTTPResult struct
    // httpClient: BoxHttpClient - the HTTP client instance
    // rawResponse: HttpResponse - raw Java HTTP response object
}

// Streaming mode (onChunk)
function(chunkNumber, content, totalBytes, httpResult, httpClient, rawResponse) {
    // chunkNumber: Integer - 1-based chunk counter
    // content: String - chunk data
    // totalBytes: Long - total bytes received so far
    // httpResult: IStruct - HTTPResult struct
    // httpClient: BoxHttpClient - the HTTP client instance
    // rawResponse: HttpResponse - raw Java HTTP response object
}
```

### onRequestStart

```boxlang
function(requestInfo) {
    // requestInfo: IStruct with keys:
    //   - result: HTTPResult struct
    //   - httpClient: BoxHttpClient instance
    //   - httpRequest: Java HttpRequest object
    //   - sse: Boolean - true if SSE mode is active
}
```

### onComplete

```boxlang
function(httpResult, response) {
    // httpResult: IStruct - complete HTTPResult struct
    // response: HttpResponse - Java HTTP response object
}
```

### onError

```boxlang
function(exception, httpResult) {
    // exception: Throwable - the exception that occurred
    // httpResult: IStruct - HTTPResult struct (partial data if available)
}
```

## SSE Event Structure

When using `onMessage`, each event received is a struct with the following keys:

```boxlang
{
    "data": "Event data (string, may contain newlines)",
    "event": "message",  // Event type (default: "message")
    "id": "123",         // Event ID (optional, for reconnection)
    "retry": 5000        // Reconnection delay in ms (optional)
}
```

## Technical Implementation

### SSE Specification Compliance

The implementation follows the [WHATWG Server-Sent Events specification](https://html.spec.whatwg.org/multipage/server-sent-events.html):

- **Field parsing**: `data:`, `event:`, `id:`, `retry:`, `comment` (`:`)
- **Multi-line data**: Multiple `data:` fields concatenated with `\n`
- **Event dispatching**: Events dispatched on empty line (`\n\n`)
- **UTF-8 BOM**: Automatic detection and removal of UTF-8 BOM (`\uFEFF`)
- **Last-Event-ID**: Sent in subsequent requests for reconnection
- **Content-Type detection**: Auto-enables SSE for `text/event-stream`

### Error Handling

Comprehensive error handling with graceful degradation:

- **Timeout handling**: 408 Request Timeout status
- **Connection failures**: 502 Bad Gateway status
- **SSE parsing errors**: Logged but don't stop stream processing
- **Callback exceptions**: Caught and logged, callbacks invoked
- **Thread interruption**: Proper cleanup and re-interruption

### Statistics Tracking

All streaming requests update client-level metrics:

- Total/successful/failed request counts
- Execution time (min/max/total)
- Bytes sent/received
- Failure type counters (timeout, connection, TLS, protocol)

## Testing

Comprehensive test suite added in `HTTPSSETest.java`:

✅ **11 tests covering:**

- SSE auto-detection from Content-Type
- Force SSE mode with `sse=true`
- `onMessage` syntactic sugar
- Event IDs and `Last-Event-ID` tracking
- Custom event types
- Multi-line data handling
- `onRequestStart` callback with SSE metadata
- `onComplete` callback
- Accumulated content in results
- Retry directive parsing
- Non-SSE streaming compatibility

All tests use WireMock for reliable HTTP stubbing.

## Breaking Changes

None. This is a backward-compatible addition. Existing HTTP component usage is unchanged.

## Migration Guide

### Before (Buffered Response)

```boxlang
bx:http url="https://api.example.com/data"
    method="GET"
    result="result"
{}

writeOutput(result.fileContent);
```

### After (Streaming Response)

```boxlang
bx:http url="https://api.example.com/data"
    method="GET"
    result="result"
    onChunk=(chunkNumber, content, totalBytes, httpResult, httpClient, rawResponse) => {
        writeOutput(content);  // Process in real-time
    }
{}
```

### Before (Manual SSE Parsing)

```boxlang
bx:http url="https://api.example.com/events" result="result" {}

// Manual parsing required
lines = listToArray(result.fileContent, chr(10));
// ... complex parsing logic ...
```

### After (Native SSE Support)

```boxlang
bx:http url="https://api.example.com/events"
    result="result"
    onMessage=(event, lastEventId, httpResult, httpClient, rawResponse) => {
        // event.data, event.event, event.id already parsed
        handleEvent(event);
    }
{}
```

## Performance Considerations

- **Memory**: Streaming mode processes data incrementally, avoiding large memory allocations
- **Latency**: Real-time processing as data arrives (no buffering delay)
- **Throughput**: Minimal overhead for SSE parsing (~microseconds per event)
- **Concurrency**: Callbacks executed on HTTP client thread pool

## Documentation Updates

- JavaDoc added/updated for all new methods
- Component attribute documentation complete
- Code examples in JavaDoc
- Comprehensive inline comments for SSE parser logic

## Related Issues

Closes #[issue-number] - Add streaming support to HTTP component
Closes #[issue-number] - Add Server-Sent Events (SSE) support

## Future Enhancements

Potential follow-up work:

- WebSocket support (bidirectional streaming)
- HTTP/3 (QUIC) support when Java adds native support
- Multipart streaming uploads
- Configurable SSE reconnection logic
- SSE connection pooling/management

---

**Author:** Luis Majano
**Date:** November 15, 2025
**Branch:** `copilot/add-http-component-chunk-support`
