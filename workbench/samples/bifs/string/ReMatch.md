### reMatchNoCase Example

Uses a regular expression (RE) to search a string for a pattern.

<a href="https://try.boxlang.io/?code=eJwrLinKzEtXsFVQMjQyNnFMSk5MdkxOSVGy5iovyixJdSnNLdBQKEr1TSxJzvDLd04sTtVQUIp21I2K1VbSUSiG6NZU0LTmAgBxlhaV" target="_blank">Run Example</a>

```java
string = "1234AbcacAcdd";
writeDump( reMatchNoCase( "[A-Z]+", string ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUAhy9U0sSc7wy3dOLE7VUFCKdtSNitVW0lFQMjRyTEpOTHZMTklRUtBU0LTmAgDzOw%2F%2B" target="_blank">Run Example</a>

```java
writeDump( REMatchNoCase( "[A-Z]+", "12AbcacAcdd" ) );

```


