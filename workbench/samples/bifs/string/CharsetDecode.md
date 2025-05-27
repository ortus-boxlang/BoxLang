### Decode a string using utf-8 back into binary encoding of the string

Use charsetDecode to decode with utf-8

<a href="https://try.boxlang.io/?code=eJxLzkgsKk4tcUlNzk9J1VBQKi4pysxLV9JRUCotSdO1UFLQtOYCAOn2CyQ%3D" target="_blank">Run Example</a>

```java
charsetDecode( "string", "utf-8" );

```

Result: [B@5d9905a6

### Decode a string using us-ascii back into binary encoding of the string

Use charsetDecode to decode with us-ascii

<a href="https://try.boxlang.io/?code=eJxLzkgsKk4tcUlNzk9J1VBQKi4pysxLV9JRUCot1k0sTs7MVFLQtOYCABNqDI4%3D" target="_blank">Run Example</a>

```java
charsetDecode( "string", "us-ascii" );

```

Result: [B@8154ffd

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwzkgsKk4tcUlNzk9J1VBQ8lRIzFVIVCguKcrMS9dT0lFQKi1J07VQUtBU0LTmAgDDLA9R" target="_blank">Run Example</a>

```java
dump( CharsetDecode( "I am a string.", "utf-8" ) );

```


