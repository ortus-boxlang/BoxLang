### Left Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFPICSnKzNVQUFIAAmc3l%2FzkYhBLSUETKKdko2TNBQCs%2FwgD" target="_blank">Run Example</a>

```java
">" & lTrim( "    CFDocs    " ) & "<";

```

Result: >CFDocs    <

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjrEOgkAQRHu%2BYryCQGHuA4ydhYWlCfXKLd4lB0f2FvHzFWKw0G52NvNmrEUrTMp4kAS6RcYc1IOQVcJwR%2Bqg%2FFSoJ4WnjMjklgcNDioU4nLkkVrORZcSjjA4c4wJTZLodoA5FNYiTTpOutUUswTl09SPFczeoMQSLldd%2Fwusu97eNuDT%2BcO5XCX01Yqrv8AXnNRKVQ%3D%3D" target="_blank">Run Example</a>

```java
// create variable with a string of text that has leading and trailing spaces
foo = " Hello World!  ";
// output variable
writeDump( "-" & foo & "-" );
// output variable without leading spaces
writeDump( "-" & LTrim( foo ) & "-" );

```


