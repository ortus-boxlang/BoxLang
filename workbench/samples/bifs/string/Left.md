### Using left() on a string

In this example we'll use left() to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyElNA5JKIRmpCoWlmcnZCklF%2BeV5Cmn5FQpZpbkFqSkK%2BWWpRQolQPmcxKpKhZT8dCUdBUNLBU0FTWsuAAH0GPg%3D" target="_blank">Run Example</a>

```java
writeOutput( left( "The quick brown fox jumped over the lazy dog", 19 ) );

```

Result: The quick brown fox

### Using left() with a negative count on a string

In this example we'll use a negative count to return part of a string.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyElNA5JKIRmpCoWlmcnZCklF%2BeV5Cmn5FQpZpbkFqSkK%2BWWpRQolQPmcxKpKhZT8dCUdBV0jUwVNBU1rLgAbLBki" target="_blank">Run Example</a>

```java
writeOutput( left( "The quick brown fox jumped over the lazy dog", -25 ) );

```

Result: The quick brown fox

### Using left() in a function

In this example we'll use left() in a function to help us to capitalize the first letter in a string.

<a href="https://try.boxlang.io/?code=eJxty7EKAjEQhOHaPMVUcgcqWFuKb3CFbS7uxYW4nptdEMV3NxY2Yjv%2FN2H2sXBCNWXJmFyS8VWQ4swWCz%2Bog9LNWen0RUZ3Q49nWCiZq8D3sTZXaLIOUbNfSKxuhsNxWGHbaI8llPP5Ty4kv2Pj689tF17hDaJGNLo%3D" target="_blank">Run Example</a>

```java

public string function capitalize( required string text ) {
	return uCase( left( arguments.TEXT, 1 ) ) & right( arguments.TEXT, len( arguments.TEXT ) - 1 );
}

```


### Using left() to test values

In this example we'll use left() to test the first five characters of a request context variable.


```java
if( listFindNoCase( "super,great,coder,rulez", left( rc.ANSWER, 5 ) ) ) {
	writeOutput( "You are an awesome developer!" );
}

```


### Using left() as a member function

 In this example we'll use left() as a member function inside a function to help us to capitalize the first letter in a string.

<a href="https://try.boxlang.io/?code=eJxlyzEOwjAMBdCZnOJPKBmIxMzMDTqwhuAGS8EU15YQiLtTBhbY3wuTHztXzKYsDaNLNb4KapnYSucHRSjdnJVOX2R0NyQ8w0rJXAVFm19IbM7D%2FjDkTqNFbJGy1zJTTFj%2FEuV2XsxflAVvPnUXXuENAPc0eg%3D%3D" target="_blank">Run Example</a>

```java

public string function capitalize( required string text ) {
	return arguments.TEXT.left( 1 ).ucase() & arguments.TEXT.right( arguments.TEXT.len() - 1 );
}

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FDISU0r0VBQSkxKTlHSUTBW0FTQtFbQ11cACnClIKsISC1JLQIq0TUEq%2BECKQKJcQEA1EkSxg%3D%3D" target="_blank">Run Example</a>

```java
dump( left( "abcd", 3 ) ); // abc
dump( left( "Peter", -1 ) );
 // Pete

```


