### isCustomFunction Example

Here we've example to check the given variable is user defined function or not.

<a href="https://try.boxlang.io/?code=eJzjSivNSy7JzM9TyK0EMTU0Faq5OItSS0qL8hQMrblqucqLMktSXUpzCzQUMoudS4tL8nPdoHo0oJoUNBU0rbkA0i0ZNg%3D%3D" target="_blank">Run Example</a>

```java

function myfunc() {
	return 1;
}
writeDump( isCustomFunction( myfunc ) );

```

Result: YES

### isCustomFunction Example

Here we've example to check the given variable is user defined function or not.

<a href="https://try.boxlang.io/?code=eJzLrUwrzUtWsFVQMlKy5iovyixJdSnNLdBQyCx2Li0uyc91A0qXZObnaSjkQpRqKmhacwEABtYSIg%3D%3D" target="_blank">Run Example</a>

```java
myfunc = "2";
writeDump( isCustomFunction( myfunc ) );

```

Result: NO

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMgsdi4tLsnPdSvNSy7JzM%2FTUChKTcwJdXFT0FTQtOYqx6eyoqICoqoktbgEKKxgC9NszRUB5BgS0A%2FTRtimCIgarjSoCMweDU2Fai7OotSS0qI8kHW1SEqArtNQcMyrVCjLz0xRACms5QIAOE1SOA%3D%3D" target="_blank">Run Example</a>

```java
writeDump( isCustomFunction( realUDF ) );
writeDump( isCustomFunction( xxx ) );
testFun = realUDF;
X = 1;
writeDump( isCustomFunction( testFun ) );
writeDump( isCustomFunction( X ) );

function realUDF() {
	return 1;
}

function xxx( Any void ) {
}

```


