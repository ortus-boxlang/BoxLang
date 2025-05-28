### jsStringFormat Example

This example illustrates use of the JSStringFormat function.

<a href="https://try.boxlang.io/?code=eJwrLinKzEtXsFVQcsxTSK1IzC3ISVUohgiWJeaUpiqUZ5ZkKCgpFZbml6SmKCkpqJekVpSoK1lzlRdllqT6l5YUlJZoKHgFB4M1ueUX5SYC%2BVAjNBU0rbkA0lYhzQ%3D%3D" target="_blank">Run Example</a>

```java
string = "An example string value with ""quoted"" 'text'";
writeOutput( JSStringFormat( string ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLilSsFVQcspPUi9WSM7PzU3NK1EoTyxWUFJydvP1USjKT84uVlRSUrLmKi%2FKLEn1Ly0pKC3RUCgG6lNTULJJKrJTUtBEk%2FQKDi4pysxLd8svyk2EKtYEqQIAhTEilA%3D%3D" target="_blank">Run Example</a>

```java
str = "Bob's comment was ""BL rocks!""";
writeOutput( str & "<br>" );
writeOutput( JSStringFormat( str ) );

```


