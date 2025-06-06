### Throw a custom exception

Use the throw function to throw a custom application exception.


```java
<bx:script>
	throw( type="MyCustomError", message="A custom error has been thrown!" );
</bx:script>

```


### Throw a custom http response exception

Use the throw function to throw a custom exception when the http response is invalid.


```java
if( !isJSON( httpResponse.FILECONTENT ) ) {
	throw( type="InvalidHTTPResponse", message="The http response was not valid JSON" );
}

```


### Additional Examples


```java
// thrown as a statement example
try {
	throw "thrown";
} catch ( e) {
	dump( var=bxcatch, label="single argument keyword" );
}

try {
	throw message = "thrown";
	detail = "deets";
	errorCode = "403";
	type = "Test";
} catch ( e) {
	dump( var=bxcatch, label="additional arguments are ignored" );
}
// use this syntax instead
try {
	throw( message="thrown", detail="deets", errorCode="403", type="Test" );
} catch ( e) {
	dump( var=bxcatch, label="script throw with arguments" );
}

```


