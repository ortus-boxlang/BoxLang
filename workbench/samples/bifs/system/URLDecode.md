### Simple urlDecode() example

Shows how it takes an input of: %21 and returns: !

<a href="https://try.boxlang.io/?code=eJwrLcpxSU3OT0nVUFBSNTJUUtC05gIAQ8QFOg%3D%3D" target="_blank">Run Example</a>

```java
urlDecode( "%21" );

```

Result: !

### Basic urlDecode() usage

In this example we demonstrate taking a URL encoded message passed on the request context and displaying it decoded.


```java
if( len( rc.MSG ) ) {
	writeOutput( encodeForHTML( urlDecode( rc.MSG ) ) );
}

```

Result: 

### urlDecode() in obfuscation

In this example we demonstrate url encoding a password before it is encrypted, and then decoding it after it is decrypted.

<a href="https://try.boxlang.io/?code=eJwrKE9RsFUoLcpxzUvOT0lNccsvyk0s0VBQUjG0UEmLU4nICUk1MVRS0LTmKi%2FKLEn1Ly0pKAXKFwD1qSkoKVgpgOUKwMak5iUXVRZAZHUUlMyjLPLT9NO9wwsKiyv0y8z8TSs9grwT84oiigssjZ1MKjw9wizNq9IsLAJtlYDqnXz8w908gz30nZ2c9QO8nYNNAxJTUjLz0kGSHq4RBF2RkpoMcQiQQV%2BHQG3G5hZg2LqkgsIWrgqHZqAwAG7SeBo%3D" target="_blank">Run Example</a>

```java
pwd = urlEncodedFormat( "$18$f^$XlTe41" );
writeOutput( pwd & " : " );
pwd = encrypt( pwd, "7Z8of/gKWpqsx/v6O5yHRKanrXsp93B4xIHV97zf88Q=", "BLOWFISH/CBC/PKCS5Padding", "HEX" );
writeOutput( pwd & " : " );
decpwd = decrypt( pwd, "7Z8of/gKWpqsx/v6O5yHRKanrXsp93B4xIHV97zf88Q=", "BLOWFISH/CBC/PKCS5Padding", "HEX" );
writeOutput( decpwd & " : " );
decpwd = urlDecode( decpwd );
writeOutput( decpwd );

```

Result: %2418%24f%5E%24XlTe41 : <some encrypted value> : %2418%24f%5E%24XlTe41 : $18$f^$XlTe41

### urlDecode() usage as a member function

In this example we demonstrate taking a URL encoded message passed on the request context and displaying it decoded using the urlDecode() member function.


```java
if( len( rc.MSG ) ) {
	writeOutput( encodeForHTML( rc.MSG.urlDecode() ) );
}

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzUvOT0lNiS8uKcrMS1ewVVDKKCkpKFY1dlQ1cgOilNQyVSPXnNLk1FQgnV%2BUDhQrAeLy1Jzk%2FFygmEtJPpCAKnABK3czMjAzUbLmSinNLdBQCA3ycUkFWaKhkIpqmaaCpjWXgr6%2BAthKK319oG49sEl6QIv0S%2FShluiW5OuChXWBCvRBhnMBACK4OpY%3D" target="_blank">Run Example</a>

```java
encoded_string = "https%3A%2F%2Fdev%2Elucee%2Eorg%2Ft%2Fwelcome%2Dto%2Dlucee%2Ddev%2F2064";
dump( URLDecode( encoded_string ) );
 // https://dev.lucee.org/t/welcome-to-lucee-dev/2064

```


