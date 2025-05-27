### Encode a binary string back to a string using hex

use binaryEncode to Encode with hex

<a href="https://try.boxlang.io/?code=eJwrVrBVSMrMSyyqdElNzk9J1VBQMjc2NzE3MrM0czUzV9JRUMpIrVBS0LTmgihzzYMoK0aSAQCzFRMv" target="_blank">Run Example</a>

```java
s = binaryDecode( "737472696E67", "hex" );
binaryEncode( s, "hex" );

```

Result: 737472696e67

### Encode a binary using UNIX UUencode (UU) back into string

use binaryEncode to Encode with UNIX UUencode (UU)

<a href="https://try.boxlang.io/?code=eJwrVrBVSMrMSyyqdElNzk9J1VBQUrMJNwyyMot0V9JRUAoNVVLQtOaCKHHNgygpRkgAAF4VEbA%3D" target="_blank">Run Example</a>

```java
s = binaryDecode( "&<W1R:6YG", "UU" );
binaryEncode( s, "UU" );

```

Result: W1R6YA==

### Encode a binary using base64 back into a string

use binaryEncode to Encode with base64

<a href="https://try.boxlang.io/?code=eJwrVrBVSMrMSyyqdElNzk9J1VBQUrMJNwyyMot0V9JRUEpKLE41M1FS0LTmgihzzYMoK0aVBAAF8xRm" target="_blank">Run Example</a>

```java
s = binaryDecode( "&<W1R:6YG", "base64" );
binaryEncode( s, "base64" );

```

Result: W1R6YA==

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLSixOjTczUbBVCMl3ArLNTDQUlDwVEnMVEhWKS4oy89L1lBQ0rbmSMvMSiyrjUxJLEiFqwXwNhSSofqCSlNLcAg0FiIRrXnJ%2BSipQGqFNR0EpI7UCaBhIrYK%2BvoKJpZGBmaGZC4g0MjA3NjcxNzKzNHM1Mzdy5QIAyXYrUA%3D%3D" target="_blank">Run Example</a>

```java
base_64 = ToBase64( "I am a string." );
binary_data = ToBinary( base_64 );
dump( BinaryEncode( binary_data, "hex" ) );
 // 4920616D206120737472696E672E

```


