# BoxLang SOAP Web Services - Quick Reference

## Creating a SOAP Client

```java
// Method 1: Using createObject()
ws = createObject("webservice", "http://example.com/service.wsdl");

// Method 2: With configuration
ws = createObject("webservice", "http://example.com/service.wsdl")
    .setTimeout(30000)                           // 30 second timeout
    .setAuthentication("user", "pass")           // HTTP Basic Auth
    .addHeader("X-API-Key", "secret")           // Custom headers
    .setSoapVersion("1.2");                      // SOAP 1.2 (default is 1.1)
```

## Invoking SOAP Operations

```java
// Method 1: Direct method call (recommended)
result = ws.methodName({ param1: value1, param2: value2 });

// Method 2: Using invoke() method
result = ws.invoke("methodName", { param1: value1, param2: value2 });

// Method 3: Using invoke component
result = invoke {
    webservice: "http://example.com/service.wsdl"
    method: "methodName"
    argumentCollection: { param1: value1, param2: value2 }
};
```

## Discovering Operations

```java
// Get list of all operations
operations = ws.listOperations();
// Returns: ["Operation1", "Operation2", "Operation3"]

// Get detailed info about an operation
opInfo = ws.getOperationInfo("methodName");
// Returns struct with:
//   - name: operation name
//   - soapAction: SOAP action header value
//   - targetNamespace: XML namespace
//   - inputParameters: array of parameter structs
//   - outputParameters: array of parameter structs
```

## Client Statistics

```java
stats = ws.getStatistics();
// Returns struct with:
//   - totalInvocations: total number of calls
//   - successfulInvocations: successful calls
//   - failedInvocations: failed calls
//   - averageResponseTime: average time per call (future)
```

## HttpService Cache Management

```java
httpService = BoxRuntime.getInstance().getHttpService();

// Check if WSDL is cached
hasCached = httpService.hasSoapClient("http://example.com/service.wsdl");

// Get cached client
ws = httpService.getSoapClient("http://example.com/service.wsdl");

// Remove from cache
httpService.removeSoapClient("http://example.com/service.wsdl");

// Clear all cached SOAP clients
httpService.clearAllSoapClients();

// Get cache statistics
count = httpService.getSoapClientCount();
stats = httpService.getSoapClientStats("http://example.com/service.wsdl");
allStats = httpService.getAllSoapClientStats();
```

## Error Handling

```java
try {
    result = ws.methodName({ param1: value });
} catch (any e) {
    // SOAP fault or network error
    println("Error: " & e.message);
    println("Detail: " & e.detail);
}
```

## Common SOAP Faults

- **Invalid operation**: Operation name not found in WSDL
- **Missing parameter**: Required parameter not provided
- **Type mismatch**: Parameter type doesn't match WSDL definition
- **SOAP fault**: Service returned a fault (check e.message for details)
- **Network error**: Connection timeout or unreachable service
- **Parse error**: Invalid WSDL or malformed response

## Best Practices

1. **Cache clients**: The HttpService automatically caches WSDL and clients - reuse them!
2. **Set timeouts**: Use `setTimeout()` to prevent hanging on slow services
3. **Error handling**: Always wrap SOAP calls in try/catch blocks
4. **Authentication**: Use `setAuthentication()` for services requiring credentials
5. **Custom headers**: Use `addHeader()` for API keys or custom authentication
6. **Check operations**: Use `listOperations()` to discover available methods
7. **SOAP version**: Most services use SOAP 1.1 (default), only change if needed

## Example: Temperature Conversion Service

```java
// Create client
ws = createObject("webservice", "http://www.w3schools.com/xml/tempconvert.asmx?WSDL");

// Convert Celsius to Fahrenheit
fahrenheit = ws.CelsiusToFahrenheit({ Celsius: "100" });
println("100°C = " & fahrenheit & "°F");

// Convert Fahrenheit to Celsius
celsius = ws.FahrenheitToCelsius({ Fahrenheit: "212" });
println("212°F = " & celsius & "°C");
```

## Example: Calculator Service

```java
// Create client
ws = createObject("webservice", "http://www.dneonline.com/calculator.asmx?WSDL")
    .setTimeout(15000);

// Perform calculations
sum = ws.Add({ intA: 5, intB: 3 });
diff = ws.Subtract({ intA: 10, intB: 4 });
product = ws.Multiply({ intA: 6, intB: 7 });
quotient = ws.Divide({ intA: 20, intB: 5 });

println("5 + 3 = " & sum);
println("10 - 4 = " & diff);
println("6 × 7 = " & product);
println("20 ÷ 5 = " & quotient);
```

## Troubleshooting

### "SOAP operation not found in WSDL"
- Check available operations: `ws.listOperations()`
- Verify operation name matches exactly (case-sensitive)

### "Connection timeout"
- Increase timeout: `ws.setTimeout(60000)` (60 seconds)
- Check if service URL is accessible

### "Invalid WSDL"
- Verify WSDL URL returns valid XML
- Check if WSDL is WSDL 1.1 format (WSDL 2.0 not yet supported)

### "Authentication failed"
- Verify credentials: `ws.setAuthentication("user", "pass")`
- Some services may require header-based auth instead

### "Type conversion error"
- Check parameter types in operation info: `ws.getOperationInfo("method")`
- Ensure parameter values match expected types

## Implementation Details

- **WSDL Support**: WSDL 1.1 (full), WSDL 2.0 (not yet)
- **SOAP Support**: SOAP 1.1 (default), SOAP 1.2 (via setSoapVersion)
- **Authentication**: HTTP Basic Auth (more types coming soon)
- **Security**: XXE protection enabled by default
- **Thread Safety**: All operations are thread-safe
- **Caching**: Automatic WSDL and client caching for performance
- **Dependencies**: Zero third-party libraries - pure JDK 21+ and BoxLang

---

For more details, see `SOAP_IMPLEMENTATION_SUMMARY.md`
