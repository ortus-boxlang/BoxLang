### isObject Example

Returns true if the parameter is a BL object. The BL object here is a Java object.

<a href="https://try.boxlang.io/?code=eJwrSsxLyc9VsFVILkpNLEn1T8pKTS7RUFDKSixLVNKB0HrFqcmlRZkllXrBIEZqEFiPkoKmXnpqiWdecUliXnIqUE%2Bwh6NhQJCfO1DGmqscqCHVv7SkoBRoXGYxzOAiiH2aICUASxQo5w%3D%3D" target="_blank">Run Example</a>

```java
random = createObject( "java", "java.security.SecureRandom" ).getInstance( "SHA1PRNG" );
writeOutput( isObject( random ) );

```

Result: true

### isObject Example for Other Data Types

Returns false if the parameter is any data type other than a BL object

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJVbBVqFbg4lTKS8xNVVKwUlDyVEjMVUhUKC4pKk0uUeKqteYqL8osSfUvLSkoLdFQyCz2T8pKTQayUqEGaCpoWnMBALczGEY%3D" target="_blank">Run Example</a>

```java
example = { 
	"name" : "I am a struct"
};
writeOutput( isObject( example ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyVkEELwjAMhe%2F%2BitDTBlLwLLuoBxHUw35BNrIto9aRtSv%2Be%2BsKIszLTjnkve%2FlJQg7OvnHkIHBikyhSidsW5jQeFLbOKXg8V71VLsMVOlHVpBDvt%2BEhfWryyGw6wBF8AWNt7Xjp13A5vWNQga7D%2FE%2F8yiEjpIF2MIFJ1yQ6h9RPLJPmnlq79joM47dFQe1Lie1YMux0apMg7bV6ZEH3zQkMVgnTsp%2FAy9re7Q%3D" target="_blank">Run Example</a>

```java
writeDump( label="String value", var=isObject( "Susi" ) );
writeDump( label="isObject() with array function", var=isObject( arrayNew( 1 ) ) );
writeDump( label="CreateObject in Java", var=isObject( createObject( "java", "java.util.HashMap" ) ) );
writeDump( label="CreateObject in Java with init()", var=isObject( createObject( "java", "java.lang.StringBuffer" ).init() ) );

```


