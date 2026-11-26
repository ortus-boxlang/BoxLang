### Check if Date is instance of java.util.Date

BoxLang dates are DateTime values, not instances of `java.util.Date`.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyCz2zCsuScxLTvVP01DIyy%2FX0NRRUMpKLEvUKy3JzNFzSSxJVVLQVNAEAB0QEhQ%3D" target="_blank">Run Example</a>

```java
writeOutput( isInstanceOf( now(), "java.util.Date" ) )

```

Result: false

### Check against Java types

```java
writeOutput( isInstanceOf( {}, "java.util.Map" ) & " " & isInstanceOf( "BoxLang", "java.util.Map" ) & " " & isInstanceOf( "BoxLang", "java.lang.String" ) )
```

Result: true false true
