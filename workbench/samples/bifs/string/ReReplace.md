### Strip Characters Using ReReplace

This example strips out all characters except a-z and 0-9.

<a href="https://try.boxlang.io/?code=eJwrSg1KLchJTE71y3dOLE7VUFAqSS0uUTA0MlZU0lFQio5L1K0y0LWMBXFA2NHHR0lB05oLAOdQDrs%3D" target="_blank">Run Example</a>

```java
reReplaceNoCase( "test 123!", "[^a-z0-9]", "", "ALL" );

```

Result: test123

### Extract Characters Using Back Reference

Uses a back reference: \1 to extract the pattern contained within the parenthesis.

<a href="https://try.boxlang.io/?code=eJwrSg1KLchJTE71y3dOLE7VUFAyNDJOTEo2MTVT0lFQijbQtYzV1ohO1K2K1daE8EDiMYZKCprWXAA%2B8RAd" target="_blank">Run Example</a>

```java
reReplaceNoCase( "123abc456", "[0-9]+([a-z]+)[0-9]+", "\1" );

```

Result: abc

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUAhyDUotyElMTvXLd04sTtVQUKqoSExKhhJKOgpKjk7OIColNU1JQVNB05qrHJ9uZ0cnxyDXEJAO5xonEOUONsTHhxjdyYlJiUWpJSAd0Y66UbGk6vdUyMkvS1XISvXxyUwtBmnMSs3J0aisAfI0Qdzk%2FPxskAzxZnkBDagkaBIAfCJliw%3D%3D" target="_blank">Run Example</a>

```java
writeDump( REReplaceNoCase( "xxabcxxabcxx", "ABC", "def" ) );
writeDump( REReplaceNoCase( "CABARET", "C|B", "G", "ALL" ) );
writeDump( REReplaceNoCase( "cabaret", "[A-Z]", "G", "ALL" ) );
writeDump( REReplaceNoCase( "I love jeLLies", "jell(y|ies)", "cookies" ) );
writeDump( REReplaceNoCase( "I love Jelly", "jell(y|ies)", "cookies" ) );

```


