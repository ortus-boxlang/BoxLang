### Simple urlDecode example

Takes %21 and returns !

```java
writeOutput( urlDecode( "%21" ) )
```

Result: !

### Plus signs and escapes

A plus sign is decoded as a space.

```java
writeOutput( urlDecode( "a+b%20c" ) )
```

Result: a b c

### Decode a full URL

```java
encoded = "https%3A%2F%2Fdev.boxlang.org%2Ft%2Fwelcome-to-boxlang-dev%2F2064"
writeOutput( urlDecode( encoded ) )
```

Result: https://dev.boxlang.org/t/welcome-to-boxlang-dev/2064

### Decode a request value

Display a URL encoded message passed on the request, safely encoded for HTML.

```java
if ( len( rc.msg ) ) {
    writeOutput( encodeForHTML( urlDecode( rc.msg ) ) )
}
```

Result: the decoded and HTML-encoded value of rc.msg
