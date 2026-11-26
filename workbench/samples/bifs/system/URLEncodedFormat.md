### Simple urlEncodedFormat example

Spaces become %20; letters, digits and the characters . - * _ are left alone.

```java
writeOutput( urlEncodedFormat( "This is a string with special character." ) )
```

Result: This%20is%20a%20string%20with%20special%20character.

### Encode a URL

```java
writeOutput( urlEncodedFormat( "https://dev.boxlang.org/t/welcome-to-boxlang-dev/2064" ) )
```

Result: https%3A%2F%2Fdev.boxlang.org%2Ft%2Fwelcome-to-boxlang-dev%2F2064

### Using the member function

```java
writeOutput( "a b&c".urlEncodedFormat() )
```

Result: a%20b%26c
