### String Example

Converts a String to a Base64-String.

<a href="https://try.boxlang.io/?code=eJwLyXdKLE41M9FQUApJLS5RCC4pysxLV1LQtOYCAHaPB%2Fo%3D" target="_blank">Run Example</a>

```java
ToBase64( "Test String" );

```

Result: VGVzdCBTdHJpbmc=

### Binary Object Example

Converts an Image Binary to a Base64-String.


```java
ToBase64( ToBinary( ImageRead( "example.jpg" ) ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FAIyXdKLE41M9FQUIIxlRQ0FTStuRT09RXC3C2dIyP8cvyyAm25AFw%2BDSQ%3D" target="_blank">Run Example</a>

```java
dump( ToBase64( "ToBase64" ) );
 // VG9CYXNlNjQ=

```


