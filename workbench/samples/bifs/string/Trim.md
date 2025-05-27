### Trim



<a href="https://try.boxlang.io/?code=eJxTslNSUFMoKcrM1VBQUgACZzeX%2FORiEEtJQRMopWSjZM0FAKIqB7c%3D" target="_blank">Run Example</a>

```java
">" & trim( "    CFDocs    " ) & "<";

```

Result: >CFDocs<

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx9jrEOwjAMRPd%2BxZGhageUD0BsDHwAErNpXRopbSrHafl8SAYYQGznO93zWYtOmJSxkji6ecbmdAQhqrj5jjBA%2BaHQkRQjRXimPgc091Ah5%2FMRF%2Bo4VkMIOMLgzN4HXIP4fgeYQ2UtQtIl6ftNtYlTPqVpaWD2BjVyuS66%2FVUou17e3wFf0Iu4qSno9gN%2FAm76TtY%3D" target="_blank">Run Example</a>

```java
// create variable with a string of text that has leading and trailing spaces
foo = " Hello World!  ";
// output variable
writeDump( "-" & foo & "-" );
// output variable without leading and trailing spaces
writeDump( "-" & Trim( foo ) & "-" );

```


