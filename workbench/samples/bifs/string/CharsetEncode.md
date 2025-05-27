### Encode a string using utf-8 back into binary encoding of the string

Use charsetEncode to Encode with utf-8

<a href="https://try.boxlang.io/?code=eJwrVrBVSM5ILCpOLXFJTc5PSdVQUCouKcrMS1fSUVAqLUnTtVBS0LTmgqpxzYOoKUaRBAAFghTZ" target="_blank">Run Example</a>

```java
s = charsetDecode( "string", "utf-8" );
charsetEncode( s, "utf-8" );

```

Result: string

### Encode a string using us-ascii back into binary encoding of the string

Use charsetEncode to Encode with us-ascii

<a href="https://try.boxlang.io/?code=eJwrVrBVSM5ILCpOLXFJTc5PSdVQUCouKcrMS1fSUVAqLdZNLE7OzFRS0LTmgipzzYMoK0aXBwClexet" target="_blank">Run Example</a>

```java
s = charsetDecode( "string", "us-ascii" );
charsetEncode( s, "us-ascii" );

```

Result: string

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLSU3OT0lNUbBVcM5ILCpOLXEBC2goKHkqJOYqJCoUlxRl5qXrKekoKJWWpOlaKCloWnOllOYWaMB0uOZBdKRAjEJSCFKqoK%2BPZpYSFwBvKCLr" target="_blank">Run Example</a>

```java
decoded = CharsetDecode( "I am a string.", "utf-8" );
dump( CharsetEncode( decoded, "utf-8" ) );
 // "I am a string"

```


